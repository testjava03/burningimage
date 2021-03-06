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
package pl.burningice.plugins.image.test

import grails.test.*
import org.springframework.mock.web.MockMultipartFile
import javax.imageio.ImageIO
import org.springframework.context.ApplicationContextAware
import org.springframework.context.ApplicationContext

/**
 * Class provide additional methods to test image upload 
 *
 * @author pawel.gdula@burningice.pl
 */
abstract class BurningImageUnitTestCase extends GrailsUnitTestCase implements ApplicationContextAware {

    protected static final def SOURCE_DIR = './resources/testImages/'

    ApplicationContext applicationContext

    protected void setUp() {
        super.setUp()
        cleanUpTestDir()
    }

    protected void cleanUpTestDir(){
        new File(RESULT_DIR).list().toList().each {
            if(it != '.svn'){
                def filePath = "${RESULT_DIR}${it}"
                println "Remove ${filePath}"
                new File(filePath).delete()
            }
        }
    }

    protected def fileExists(fileName){
        println "search for file ${RESULT_DIR}${fileName}"
        new File("${RESULT_DIR}${fileName}").exists()
    }

    protected def getFilePath(fileName){
        "${SOURCE_DIR}${fileName}"
    }

    protected def getFile(fileName, dir = null){
        ImageIO.read(new File("${dir ?: RESULT_DIR}${fileName}"))
    }

    protected def getEmptyMultipartFile(){
        new MockMultipartFile('empty', new byte[0])
    }

    protected def getMultipartFile(fileName){
        def fileNameParts = fileName.split(/\./)
        def contentTypes = ['jpg':'image/jpeg', 'png':'image/png', 'gif':'image/gif', 'bmp':'image/bmp']
        new MockMultipartFile(fileNameParts[0],
                              fileName,
                              contentTypes[fileNameParts[1]],
                              new FileInputStream(getFilePath(fileName)))
    }

    protected def getAbsolutePath(String uploadDir){
        applicationContext.getResource(uploadDir).getFile().toString()
    }
}