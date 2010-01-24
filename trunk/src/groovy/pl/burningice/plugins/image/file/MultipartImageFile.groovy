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

package pl.burningice.plugins.image.file

import org.springframework.web.multipart.MultipartFile
import javax.media.jai.*
import com.sun.media.jai.codec.*

/**
 * Representing image file upladed na server as a MultipartFile
 *
 * @author pawel.gdula@burningice.pl
 */
private class MultipartImageFile extends ImageFile {

    /**
     * Class constructor
     *
     * @param source Uploaded image, source for this class
     * @return MultipartImageFile
     */
    def MultipartImageFile(MultipartFile source) {
        this.source = source
    }

    /**
     * @see ImageFile#getAsJaiStream()
     */
    def getAsJaiStream() {
        JAI.create("stream", new ByteArraySeekableStream(source.bytes))
    }

    /**
     * @see ImageFile#getSourceFileName()
     */
    def getSourceFileName() {
        source.originalFilename
    }

    /**
     * Transform self to LocalImageFile object
     * Save buffered source on this and create LocalImageFile as a result
     * with source as saved file
     *
     * @param String outputFilePath Path where buffered data should be stored
     * @return LocalImageFile
     */
    def asLocal(resultDir) {
        def outputFilePath = "${resultDir}/${name}"
        def outputFile = new FileOutputStream(outputFilePath);
        JAI.create('encode', getAsJaiStream(), outputFile, encoder, null);
        outputFile.close()
        ImageFileFactory.produce(new File(outputFilePath))
    }
}