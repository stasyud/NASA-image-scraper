package com.sl.nasa.scraper


import groovyx.gpars.GParsExecutorsPool
import org.ccil.cowan.tagsoup.Parser

import java.util.concurrent.atomic.AtomicInteger

/**
 * Created by Slavik on 16.10.15.
 */
class NASAScraper {
    final static def ROOT_URL = "http://grin.hq.nasa.gov"
    final static def CATEGORIES_URL = ROOT_URL + "/subject-all.html"
    AtomicInteger counter = new AtomicInteger(0)
    static def totalImagesCount = 0

    final static def DESTINATION_FOLDER = "e:/NASA/"

    public static void main(String[] args) {
        def start = System.currentTimeMillis()
        def scraper = new NASAScraper()
        def categoryMap = scraper.parsePage(CATEGORIES_URL)
        totalImagesCount = categoryMap.collect { it.value.size() }.sum()
        def imagesList = []
        categoryMap.each { key, value ->
            value.each { imagesList << new Expando(url: it, category: key) }
        }
        scraper.downloadImages(imagesList)
        start = System.currentTimeMillis() - start
        println("Scraping took: ${start} ms")
    }

    /**
     * Just a counter, that shows current progress of downloading images
     */
    def incCounter() {
        println("Downloaded: " + counter.incrementAndGet() + "; out of ${totalImagesCount}")
    }

    /**
     * Returns map of categories with corresponding image URLs per category
     * @param pageURL
     * @return
     */
    def parsePage(String pageURL) {
        def parser = new XmlSlurper(new Parser())
        def page = parser.parse(pageURL)
        def pageURLs = page.depthFirst().findAll { it.name() == 'a' && it.@accesskey == 'z' }
        def categoryMap = [:]
        GParsExecutorsPool.withPool(20) {// number of threads depends on your connection bandwidth
            pageURLs.eachParallel { url ->
                def anchorURL = url.@href.text().replaceAll("_1", "")
                categoryMap[url.text()] = getImageURLs(ROOT_URL + anchorURL, url.text())
            }
        }

        return categoryMap
    }

    /**
     * Checks whether given URL contains proper link to the image and not to default "not found" image
     */
    static def boolean isSuitableForParsing(String pageURL) {
        pageURL.startsWith("/IMAGES/LARGE/") && !pageURL.contains("not_available")
    }

    /**
     * Parses given page and returns all found image URLs as a list
     */
    def getImageURLs(String pageURL, String category) {
        println("Parsing page: ${category}")
        category = category.replaceAll("(\\\\|//|\\.|,|:|;| )?", "")
        def parser = new XmlSlurper(new Parser())

        def page = null
        try {
            page = parser.parse(pageURL)
        } catch (Exception e) {
            if (pageURL.contains("_1")) return
            //fallback in case page doesn't exist - it tries to scrape initial page
            return getImageURLs(pageURL.replaceAll("\\.html", "_1.html"), category)
        }
        return page.depthFirst().findAll {
            it.name() == 'a' && it.@href != null && isSuitableForParsing(it.@href.text())
        }
    }

    /**
     * Downloads file by given URL
     * @param fileURL - URL of file to download
     * @param category - name of current category, it is used to create corresponding directory
     */
    def downloadFile(String fileURL, String category) {
        def dir = new File(DESTINATION_FOLDER + category + "/")
        if (!dir.exists()) dir.mkdirs()

        // check whether file already exists
        if (new File(DESTINATION_FOLDER + category + "/" + fileURL.split("/")[-1]).exists()) return;
        try {
            new File(DESTINATION_FOLDER + category + "/" + fileURL.split("/")[-1]).withOutputStream { out ->
                out << new URL(fileURL).openStream()
            }
            incCounter()
        } catch (Exception e) {
            println("Exception while downloading file: ${fileURL}; " + e)
        }
    }

    /**
     * Downloads all the images from given list of images, saving them to the specific category
     * Downloading process is performed in several parallel threads, it allows to decrease total time of downloading
     */
    def downloadImages(imagesList) {
        GParsExecutorsPool.withPool(20) {// number of threads depends on your connection bandwidth
            imagesList.eachParallel { image ->
                downloadFile(ROOT_URL + image.url.@href, image.category)
            }
        }
    }
}
