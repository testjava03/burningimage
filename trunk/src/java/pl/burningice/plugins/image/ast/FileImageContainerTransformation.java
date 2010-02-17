/*
Copyright (c) 2009 Pawel Gdula <pawel.gdula@burningice.pl>

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in
all copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
THE SOFTWARE.
*/
package pl.burningice.plugins.image.ast;

import org.codehaus.groovy.ast.*;
import org.codehaus.groovy.transform.*;
import org.codehaus.groovy.control.*;
import org.codehaus.groovy.ast.expr.*;
import org.codehaus.groovy.ast.stmt.*;
import java.lang.reflect.Modifier;
import org.codehaus.groovy.syntax.*;
import org.objectweb.asm.Opcodes;
import org.apache.commons.lang.StringUtils;
import pl.burningice.plugins.image.ast.intarface.FileImageContainer;
import java.util.List;
import org.springframework.web.multipart.MultipartFile;
import pl.burningice.plugins.image.validator.ImageValidator;

/**
 * Object executer tranformation of object marked by FileImageContainer annotation
 *
 * @author pawel.gdula@burningice.pl
 */
@GroovyASTTransformation(phase = CompilePhase.CANONICALIZATION)
public class FileImageContainerTransformation implements ASTTransformation, Opcodes {

    private static final String DEFAULT_FIELD_NAME = "image";

    public void visit(ASTNode[] nodes, SourceUnit sourceUnit) {
        AnnotationNode annotation = (AnnotationNode) nodes[0];
        String fieldName = DEFAULT_FIELD_NAME;
        ConstantExpression fieldExpression = (ConstantExpression)annotation.getMember("field");

        if (fieldExpression != null){
            fieldName = fieldExpression.getValue().toString();
        }

        for (ASTNode aSTNode : nodes) {
            if (!(aSTNode instanceof ClassNode)) {
                continue;
            }

            try {
             transform((ClassNode) aSTNode, fieldName);
            }
            catch(Exception exception){
                log(exception.toString());
            }

            break;
        }
    }

    private void transform(ClassNode node, String fieldName) {
        log("start transforming: " + node + " with field " + fieldName);

        // implements interface
        node.addInterface(new ClassNode(FileImageContainer.class));

        // imageExtension field
        FieldNode imageExtension = new FieldNode("imageExtension", Modifier.PRIVATE, new ClassNode(String.class), new ClassNode(node.getClass()), null);
        node.addField(imageExtension);
        addGetter(imageExtension, node);
        addSetter(imageExtension, node);
        addNullableConstraint(node, "imageExtension");

        // field where image will be binded
        FieldNode imageBindField = new FieldNode(fieldName, Modifier.PRIVATE, new ClassNode(MultipartFile.class), new ClassNode(node.getClass()), null);
        node.addField(imageBindField);
        addGetter(imageBindField, node);
        addSetter(imageBindField, node);
        addTransientValue(node, fieldName);
        addImageValidator(node, fieldName);

        if (fieldName.equals(DEFAULT_FIELD_NAME)){
            return;
        }

        // additional fields/methods in case when field name is different than default
        // we want to have possibilty to get binded image
        addTransientValue(node, DEFAULT_FIELD_NAME);
        addGetter(DEFAULT_FIELD_NAME, imageBindField, node);
    }

    public void addTransientValue(ClassNode node, String transientValue){
        FieldNode transientField = getTransientsField(node);
        ListExpression listValues = (ListExpression)transientField.getInitialExpression();
        listValues.addExpression(new ConstantExpression(transientValue));
    }

    public FieldNode getTransientsField(ClassNode node){
        FieldNode transientFields = node.getDeclaredField("transients");

        if (transientFields != null) {
            return transientFields;
        }

        transientFields = new FieldNode("transients",
                                       Modifier.PRIVATE | Modifier.STATIC,
                                       new ClassNode(List.class),
                                       new ClassNode(node.getClass()),
                                       new ListExpression());
        node.addField(transientFields);
        addGetter(transientFields, node, Modifier.PUBLIC | Modifier.STATIC);
        addSetter(transientFields, node, Modifier.PUBLIC | Modifier.STATIC);
        return transientFields;
    }

    public void addImageValidator(ClassNode classNode, String fieldName) {
        FieldNode closure = classNode.getDeclaredField("constraints");

        if (closure != null) {

            ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
            BlockStatement block = (BlockStatement) exp.getCode();

            if (!hasFieldInClosure(closure, fieldName)) {
                Variable image = new VariableExpression("image");
                Variable imageContainer = new VariableExpression("imageContainer");

                StaticMethodCallExpression closureMethodCall = new StaticMethodCallExpression(new ClassNode(ImageValidator.class),
                                                                                              "validate",
                                                                                              new ArgumentListExpression((VariableExpression)image,
                                                                                                                         (VariableExpression)imageContainer));

                BlockStatement closureBody = new BlockStatement(new Statement[]{new ReturnStatement(closureMethodCall)},
                                                                new VariableScope());

                Parameter[] closureParameters = {new Parameter(new ClassNode(MultipartFile.class), "image"),
                                                 new Parameter(new ClassNode(FileImageContainer.class), "imageContainer")};

                VariableScope scope = new VariableScope();
                scope.putDeclaredVariable(image);
                scope.putDeclaredVariable(imageContainer);
                
                ClosureExpression validator = new ClosureExpression(closureParameters, closureBody);
                validator.setVariableScope(scope);

                NamedArgumentListExpression namedarg = new NamedArgumentListExpression();
                namedarg.addMapEntryExpression(new ConstantExpression("validator"), validator);

                MethodCallExpression constExpr = new MethodCallExpression(VariableExpression.THIS_EXPRESSION,
                                                                          new ConstantExpression(fieldName),
                                                                          namedarg);
                block.addStatement(new ExpressionStatement(constExpr));
            }
        }

    }

    public void addNullableConstraint(ClassNode classNode, String fieldName) {
        FieldNode closure = classNode.getDeclaredField("constraints");
        if (closure != null) {

            ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
            BlockStatement block = (BlockStatement) exp.getCode();

            if (!hasFieldInClosure(closure, fieldName)) {
                NamedArgumentListExpression namedarg = new NamedArgumentListExpression();
                namedarg.addMapEntryExpression(new ConstantExpression("nullable"), new ConstantExpression(true));
                namedarg.addMapEntryExpression(new ConstantExpression("blank"), new ConstantExpression(true));
                MethodCallExpression constExpr = new MethodCallExpression(
                        VariableExpression.THIS_EXPRESSION,
                        new ConstantExpression(fieldName),
                        namedarg);
                block.addStatement(new ExpressionStatement(constExpr));
            }
        }
    }

    public boolean hasFieldInClosure(FieldNode closure, String fieldName) {
        if (closure != null) {
            ClosureExpression exp = (ClosureExpression) closure.getInitialExpression();
            BlockStatement block = (BlockStatement) exp.getCode();
            List<Statement> ments = block.getStatements();
            for (Statement expstat : ments) {
                if (expstat instanceof ExpressionStatement && ((ExpressionStatement) expstat).getExpression() instanceof MethodCallExpression) {
                    MethodCallExpression methexp = (MethodCallExpression) ((ExpressionStatement) expstat).getExpression();
                    ConstantExpression conexp = (ConstantExpression) methexp.getMethod();
                    if (conexp.getValue().equals(fieldName)) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private void addGetter(FieldNode fieldNode, ClassNode owner) {
        addGetter(fieldNode.getName(), fieldNode, owner, ACC_PUBLIC);
    }

    private void addGetter(String name, FieldNode fieldNode, ClassNode owner) {
        addGetter(name, fieldNode, owner, ACC_PUBLIC);
    }

    private void addGetter(FieldNode fieldNode, ClassNode owner, int modifier) {
        addGetter(fieldNode.getName(), fieldNode, owner, modifier);
    }

    private void addGetter(String name, FieldNode fieldNode, ClassNode owner, int modifier) {
        ClassNode type = fieldNode.getType();
        String getterName = "get" + StringUtils.capitalize(name);
        owner.addMethod(getterName,
                modifier,
                nonGeneric(type),
                Parameter.EMPTY_ARRAY,
                null,
                new ReturnStatement(new FieldExpression(fieldNode)));
    }

    private void addSetter(FieldNode fieldNode, ClassNode owner) {
        addSetter(fieldNode, owner, ACC_PUBLIC);
    }

    private void addSetter(FieldNode fieldNode, ClassNode owner, int modifier) {
        ClassNode type = fieldNode.getType();
        String name = fieldNode.getName();
        String setterName = "set" + StringUtils.capitalize(name);
        owner.addMethod(setterName,
                modifier,
                ClassHelper.VOID_TYPE,
                new Parameter[]{new Parameter(nonGeneric(type), "value")},
                null,
                new ExpressionStatement(
                new BinaryExpression(
                new FieldExpression(fieldNode),
                Token.newSymbol(Types.EQUAL, -1, -1),
                new VariableExpression("value"))));
    }

    private ClassNode nonGeneric(ClassNode type) {
        if (type.isUsingGenerics()) {
            final ClassNode nonGen = ClassHelper.makeWithoutCaching(type.getName());
            nonGen.setRedirect(type);
            nonGen.setGenericsTypes(null);
            nonGen.setUsingGenerics(false);
            return nonGen;
        } else {
            return type;
        }
    }

    private void log(String message) {
        System.out.println("[Burining Image] " + message);
    }
}

