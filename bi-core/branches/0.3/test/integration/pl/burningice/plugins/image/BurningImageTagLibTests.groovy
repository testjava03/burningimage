package pl.burningice.plugins.image

import pl.burningice.plugins.image.ast.*
import grails.test.GroovyPagesTestCase
import org.codehaus.groovy.grails.commons.ConfigurationHolder
import org.codehaus.groovy.grails.web.taglib.exceptions.GrailsTagException

/**
 *
 * @author pawel.gdula@burningice.pl
 */
class BurningImageTagLibTests extends GroovyPagesTestCase {

    protected void setUp() {
        super.setUp()
        ConfigurationHolder.config = new ConfigObject()
    }

    void testResourceAbsolutePath() {
        def template = '<bi:resource size="${size}" bean="${bean}" />'
        def bean = new TestDomain(imageExtension:'jpg')
        def size = 'small'

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:null, bean:null] )
        }

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:size, bean:null] )
        }

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:null, bean:bean] )
        }

        def result = applyTemplate(template, [size:size, bean:bean])
        assertEquals '', result

        bean.save(flush:true)

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:size, bean:bean])
        }

        ConfigurationHolder.config.bi.TestDomain = [
            outputDir: ['path':'/var/home/gdulus/Workspace/Grails/0.3/web-app/upload/', 'alias':'/upload/'],
            prefix: 'prefixName',
            images: ['small':[scale:[width:100, height:100, type:null]]]
        ]

        result = applyTemplate(template, [size:size, bean:bean])
        assertEquals "/upload/prefixName-${bean.ident()}-small.jpg", result
    }

    void testResource() {
        def template = '<bi:resource size="${size}" bean="${bean}" />'
        def bean = new TestDomain(imageExtension:'jpg')
        def size = 'small'

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:null, bean:null] )
        }

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:size, bean:null] )
        }

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:null, bean:bean] )
        }

        def result = applyTemplate(template, [size:size, bean:bean])
        assertEquals '', result

        bean.save(flush:true)

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:size, bean:bean])
        }

        ConfigurationHolder.config.bi.TestDomain = [
            outputDir: '/relative/path/to/dir/',
            prefix: 'prefixName',
            images: ['small':[scale:[width:100, height:100, type:null]]]
        ]

        result = applyTemplate(template, [size:size, bean:bean])
        assertEquals "/relative/path/to/dir/prefixName-${bean.ident()}-small.jpg", result
    }

    void testImage() {
        def template = '<bi:img size="${size}" bean="${bean}" />'
        def bean = new TestDomain(imageExtension:'jpg')
        def size = 'small'

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:null, bean:null] )
        }

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:size, bean:null] )
        }

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:null, bean:bean] )
        }

        def result = applyTemplate(template, [size:size, bean:bean])
        assertEquals '', result

        bean.save(flush:true)

        shouldFail(GrailsTagException){
            applyTemplate(template, [size:size, bean:bean])
        }

        ConfigurationHolder.config.bi.TestDomain = [
            outputDir: '/',
            prefix: null,
            images: ['small':[scale:[width:100, height:100, type:null]]]
        ]

        result = applyTemplate(template, [size:size, bean:bean])
        assertEquals "<img src=\"/${bean.ident()}-small.jpg\" />", result

        template = '<bi:img size="${size}" bean="${bean}" alt="${alt}" id="${id}" onclick="${onclick}" title="${title}" name="${name}" />'
        result = applyTemplate(template, [size:size, bean:bean, alt:'img-alt', id:'img-id', onclick:'alert()', title:'img-title', name:'img-name'])
        assertEquals "<img src=\"/${bean.ident()}-small.jpg\" alt=\"img-alt\" id=\"img-id\" name=\"img-name\" onclick=\"alert()\" title=\"img-title\"/>", result
    }

    void testHasImage() {
        def template = '<bi:hasImage bean="${bean}">test</bi:hasImage>'

        def result = applyTemplate(template, [bean:null])
        assertEquals '', result

        result = applyTemplate(template, [bean:new TestDomain()])
        assertEquals '', result

        result = applyTemplate(template, [bean:new TestDomain(imageExtension:'jpg')])
        assertEquals 'test', result
    }

    void testWithHasMany() {
        ConfigurationHolder.config.bi.TestDomainSecond = [
            outputDir: '/relative/path/to/dir/',
            prefix: 'prefixName',
            images: ['small':[scale:[width:100, height:100, type:null]]]
        ]

        ConfigurationHolder.config.bi.Underscored = [
            outputDir: '/relative/path/to/underscored/dir',
            prefix: 'underscored',
            images: ['small':[scale:[width:100, height:100, type:null]]]
        ]


        def template = '<bi:resource size="${size}" bean="${bean}" />'
        def size = 'small'

        def testMain = new TestDomainSecond(email:'main@a.pl')
        def image1 = new TestDomainSecond(email:'img1@a.pl', imageExtension:'jpg')
        def image2 = new TestDomainSecond(email:'img2@a.pl', imageExtension:'jpg')
        def image3 = new TestDomainSecond(email:'img3@a.pl', imageExtension:'jpg')
        def image4 = new Underscored_Test_Domain(imageExtension:'jpg')
        def image5 = new Underscored_Test_Domain(imageExtension:'jpg')
        testMain.addToImages(image1)
        testMain.addToImages(image2)
        testMain.addToImages(image3)
        testMain.addToUndersored(image4)
        testMain.addToUndersored(image5)
        testMain.addToImagesSet(image2) 
        assertNotNull testMain.save()

        def result = applyTemplate(template, [size:size, bean:testMain.images[0]])
        assertEquals "/relative/path/to/dir/prefixName-${image1.ident()}-small.jpg", result

        result = applyTemplate(template, [size:size, bean:testMain.images[1]])
        assertEquals "/relative/path/to/dir/prefixName-${image2.ident()}-small.jpg", result

        result = applyTemplate(template, [size:size, bean:testMain.images[2]])
        assertEquals "/relative/path/to/dir/prefixName-${image3.ident()}-small.jpg", result

        result = applyTemplate(template, [size:size, bean:testMain.undersored[0]])
        assertEquals "/relative/path/to/underscored/dir/underscored-${image4.ident()}-small.jpg", result

        result = applyTemplate(template, [size:size, bean:testMain.undersored[1]])
        assertEquals "/relative/path/to/underscored/dir/underscored-${image5.ident()}-small.jpg", result

        testMain.imagesSet.each {it ->
            result = applyTemplate(template, [size:size, bean:it])
            assertEquals "/relative/path/to/dir/prefixName-${image2.ident()}-small.jpg", result
        }


    }
}
