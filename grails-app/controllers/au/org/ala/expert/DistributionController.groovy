package au.org.ala.expert

import org.apache.commons.httpclient.HttpClient
import org.apache.commons.httpclient.methods.GetMethod
import org.h2.store.fs.FileUtils

import javax.imageio.ImageIO
import java.awt.*
import java.awt.image.FilteredImageSource
import java.awt.image.ImageFilter
import java.awt.image.ImageProducer
import java.awt.image.RGBImageFilter

class DistributionController {

    def path = "/data/expert/cache"

    //extents are the same as the base image: conf/images/distributionBackground.png
    private double minX = 110.911;
    private double minY = -44.778;
    private double maxX = 156.113;
    private double maxY = -9.221;
    def backgroundImage
    def height
    def width

    //sld_body param for geoserver
    def COLOUR = "00a0b0"
    def STYLE = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><StyledLayerDescriptor version=\"1.0.0\" " +
            "xmlns=\"http://www.opengis.net/sld\">" +
            "<NamedLayer><Name>ALA:Distributions</Name>" +
            "<UserStyle><FeatureTypeStyle><Rule><Title>Polygon</Title><PolygonSymbolizer><Fill>" +
            "<CssParameter name=\"fill\">#" + COLOUR + "</CssParameter></Fill>" +
            "</PolygonSymbolizer></Rule></FeatureTypeStyle></UserStyle></NamedLayer></StyledLayerDescriptor>";

    def metadataService

    Thread rebuild = new Thread() {
        public void run() {
            if (backgroundImage == null) {
                initBackgroundImage()
            }

            def toReplace = []

            //generate missing images before replacing existing images
            def all = metadataService.getAll()
            all.each { item ->
                if (getImageFile(item.geom_idx).exists()) {
                    toReplace << item
                } else {
                    updateImage(item)
                }
            }

            //replace existing images
            toReplace.each { id ->
                updateImage(id)
            }
        }
    }

    Thread buildMissing = new Thread() {
        public void run() {
            if (backgroundImage == null) {
                initBackgroundImage()
            }

            //generate missing images before replacing existing images
            def all = metadataService.getAll()
            all.each { item ->
                if (!getImageFile(item.geom_idx).exists()) {
                    updateImage(item)
                }
            }
        }
    }

    /**
     * start the distribution images thread
     */
    def rebuild() {
        rebuild.start()

        render "rebuilding"
    }

    /**
     * get a distribution image
     */
    def show() {
        def file = getImageFile(params.id)

        def ids = metadataService.getAllIds()
        def idInt = Integer.parseInt(params.id)

        if (!file.exists() && ids.contains(idInt)) {
            //update one image
//            metadataService.getAll().each { item ->
//                if (idInt == item.geom_idx) {
//                    updateImage(item)
//                }
//            }

            //a distribution image is missing, build missing images
            buildMissing.start()
        }

        if (file.exists()) {
            render file: new FileInputStream(file), contentType: 'image/png'
        } else {
            render "not found"
        }
    }

    def getImageFile(id) {
        new File(path + "/" + id + ".png")
    }

    def updateImage(item) {
        if (backgroundImage == null) {
            initBackgroundImage()
        }
        def file = getImageFile(item.geom_idx)

        def url = item.wmsurl +
                "&bbox=" + minX + "," + minY + "," + maxX + "," + maxY +
                "&width=" + width + "&height=" + height +
                "&srs=EPSG:4326" +
                "&sld_body=" + URLEncoder.encode(STYLE, "UTF-8")

        log.debug("fetching image with url " + url)

        //geoserver image
        Image image = null
        InputStream is = null
        try {
            HttpClient client = new HttpClient()
            GetMethod get = new GetMethod(url)

            int result = client.executeMethod(get)

            File tmpFile = File.createTempFile("image", ".png")
            OutputStream os = new FileOutputStream(tmpFile)
            os.write(get.getResponseBody())
            os.close();
            image = ImageIO.read(tmpFile)
            if (image == null) {
                log.error("Could not load image at: " + url)
            }
            tmpFile.delete()
        } catch (IOException ex) {
            log.error("failed to load image from url: " + url, ex)
        } finally {
            if (is != null) {
                try {
                    is.close()
                } catch (IOException ex) {
                    log.error("failed to close url: " + url, ex)
                }
            }
        }

        //get background and merge
        Image img = ImageIO.read(this.class.classLoader.getResourceAsStream("images/distributionBackground.png"));
        def g = (Graphics2D) img.getGraphics();
        g.drawImage(makeColorTransparent(image, Color.WHITE), 0, 0, null);

        //save
        ImageIO.write(img, "png", file);
    }

    def makeColorTransparent(im, color) {
        ImageFilter filter = new RGBImageFilter() {

            // the color we are looking for... Alpha bits are set to opaque
            public int markerRGB = color.getRGB() | 0xFF000000;

            public final int filterRGB(int x, int y, int rgb) {
                if ((rgb | 0xFF000000) == markerRGB) {
                    // Mark the alpha bits as zero - transparent
                    return 0x00FFFFFF & rgb;
                    //return (rgb) | (rgb << 8) | (rgb << 16) | 0xff000000;
                } else {
                    // nothing to do
                    return rgb;
                }
            }
        };

        ImageProducer ip = new FilteredImageSource(im.getSource(), filter);
        return Toolkit.getDefaultToolkit().createImage(ip);
    }

    def initBackgroundImage() {
        //attempt to create image cache dir if it is missing
        def dir = new File(path)
        if (!dir.exists()) {
            FileUtils.createDirectories(path)
        }
        backgroundImage = ImageIO.read(this.class.classLoader.getResourceAsStream("images/distributionBackground.png"))
        height = backgroundImage.getHeight()
        width = backgroundImage.getWidth()
    }

}