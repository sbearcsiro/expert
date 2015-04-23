package au.org.ala.expert

import grails.converters.JSON
import org.codehaus.groovy.grails.commons.ConfigurationHolder

class TaxonController {

    def resultsCacheService, webService, bieService

    //static defaultAction = "index"

    static timer = 0

    def index = {}

    /**
     * Do logouts through this app so we can invalidate the session.
     *
     * @param casUrl the url for logging out of cas
     * @param appUrl the url to redirect back to after the logout
     */
    def logout = {
        session.invalidate()
        redirect(url: "${params.casUrl}?url=${params.appUrl}")
    }

    /**
     * Displays a page of the results specified by the results key.
     *
     * @params key the identifier of the results set to display
     * @param start the pagination index of the first item to display (optional defaults to 0)
     * @param pageSize the number of items to display per page (optional defaults to 10)
     * @param sortBy the property to sort on (optional)
     * @param sortOrder normal or reverse (optional defaults to normal)
     * @param debugModel
     *
     * @return model for a page of results
     */
    def view(String key) {

        //params.each { println it }

        // retrieve the required page from the results cache
        def data = resultsCacheService.get(key)

        // check for errors
        if (data == null) {
            render view: 'error', model: [message   : "No data for key " + key,
                                          searchPage: grailsApplication.config.grails.serverURL]
        } else {

            def results = buildFamilyHierarchy(data.list)

            // sort by
            def sortBy = params.sortBy ?: 'name'
            results.sort { it[sortBy] }

            // sort order
            if (params.sortOrder == 'reverse') {
                results = results.reverse()
            }

            // pagination
            def total = results.size()
            def start = params.start ? params.start as int : 0
            def pageSize = params.pageSize ? params.pageSize as int : 10
            if (results) {
                results = results[start..(Math.min(start + pageSize, total) - 1)]
            }

            // inject remaining metadata only for the families to be displayed
            results = bieService.injectGenusMetadata(results)

            def model = [list            : results, total: total, rank: 'family', parentTaxa: "", key: key,
                         queryDescription: data.queryDescription, start: start, pageSize: pageSize,
                         sortBy          : sortBy, sortOrder: params.sortOrder, query: data.list.query,
                         searchPage      : grailsApplication.config.grails.serverURL]

            if (params.debugModel == 'true') {
                render model as JSON
            } else {
                render(view: 'list', model: model)
            }
        }
    }

    /**
     * Displays a paginated list of species for the results specified by the results key.
     *
     * @params key the identifier of the results set to display
     * @params genus the name of a single genus to display (acts as a filter on the specified results)
     * @param taxa a list of family names (as this stage), comma separated
     * @param start the pagination index of the first taxon to display
     * @param pageSize the number of taxa to display per page
     * @param sortBy the column to sort on
     * @param sortOrder normal or reverse
     * @param debugModel
     *
     */
    def species(String key, String genus) {

        //params.each { println it }

        // retrieve the required page from the results cache
        def data = resultsCacheService.get(key)
        if (!data) {
            render view: 'error', model: [message   : "No data for key " + key,
                                          searchPage: grailsApplication.config.grails.serverURL]
        } else {
            def results = data.list.results

            // apply optional filter by genus
            if (genus) {
                results = results.findAll { it.genus == genus }
            }

            // sort by
            def sortBy = params.sortBy ?: 'taxa'

            results.sort(sortBy == 'taxa' ? taxaSort : { it[sortBy] })

            // sort order
            if (params.sortOrder == 'reverse') {
                results = results.reverse()
            }

            // filter by taxa
            if (params.taxa) {
                results = filterList(results, params.taxa.tokenize(','))
            }

            // pagination
            def total = results.size()
            def start = params.start ? params.start as int : 0
            def pageSize = params.pageSize ? params.pageSize as int : 10
            if (results) {
                results = results[start..(Math.min(start + pageSize, total) - 1)]
            }

            // inject remaining metadata only for species to be displayed
            results = bieService.injectSpeciesMetadata(results)

            def model = [list            : results, total: total, taxa: params.taxa, start: start, key: key,
                         queryDescription: data.queryDescription, pageSize: pageSize, sortBy: sortBy,
                         sortOrder       : params.sortOrder, query: data.list.query, rank: 'species',
                         searchPage      : grailsApplication.config.grails.serverURL]

            if (genus) {
                model.put 'genus', genus
            }

            if (params.debugModel == 'true') {
                render model as JSON
            } else {
                render(view: 'species', model: model)
            }
        }
    }

    /**
     * Displays a paginated list of species for the results specified by the results key.
     *
     * @params key the identifier of the results set to display
     * @param taxa a list of species names, comma separated
     * @param sortBy the column to sort on
     * @param sortOrder normal or reverse
     * @param debugModel
     */
    def data(String key) {
        // retrieve the required page from the results cache
        def data = resultsCacheService.get(key)
        if (!data) {
            render view: 'error', model: [message   : "No data for key " + key,
                                          searchPage: grailsApplication.config.grails.serverURL]
        } else {
            def results = data.list.results

            // sort by
            def sortBy = params.sortBy ?: 'taxa'
            results.sort(sortBy == 'taxa' ? taxaSort : { it[sortBy] })

            // sort order
            if (params.sortOrder == 'reverse') {
                results = results.reverse()
            }

            // filter by taxa
            if (params.taxa) {
                def filters = params.taxa.tokenize(',')
                results = results.findAll { it.spcode.toString() in filters }
            }

            def model = [list            : results, total: results.size(), taxa: params.taxa, key: key,
                         queryDescription: data.queryDescription, sortBy: sortBy,
                         sortOrder       : params.sortOrder, query: data.list.query,
                         searchPage      : grailsApplication.config.grails.serverURL]

            if (params.debugModel == 'true') {
                render model as JSON
            } else {
                render(view: 'data', model: model)
            }
        }
    }

    def taxaSort = { a, b ->
        (a.family <=> b.family) ?: (a.genus <=> b.genus) ?: (a.name <=> b.name)
    }

    def filterList(list, filters) {
        return list.findAll { it.family in filters }
    }

    // dummy service to return a list of taxa from an id
    def lookupTaxonList(int id) {
        switch (id) {
            case 1: return [[rank: 'class', name: 'Insecta']]
            case 2: return [[rank: 'class', name: 'Chondrichthyes']]
            case 3: return [[rank: 'order', name: 'Lamniformes']]
            case 4: return [[rank: 'genus', name: 'Notomys']]
            default: return []
        }
    }

    def lookupRegion(search) {
        if (!search) {
            return ""
        }
        switch (search) {
            case -12..1: return 'Central Eastern Province'
            case 2: return 'Carnarvon'
            case 3: return 'Tasmanian Shelf Province'
            default: return ""
        }
    }

    def listTargetRankFromSearch(search, targetRank, start, pageSize) {
        def query = ""
        switch (search) {
            case 1: query = 'q=imcra:"Central%20Eastern%20Province"&fq=species_group:Fish'; break;
            case 2: query = 'q=ibra:"Carnarvon"&fq=species_group:Fish'; break;
            case 3: query = 'q=ibra:"Tasmanian%20Shelf%20Province"&fq=species_group:Fish'; break;
            default: query = 'q=imcra:"Central%20Eastern%20Province"&fq=species_group:Fish'; break;
        }
        query += "&facets=${targetRank == 'order' ? 'bioOrder' : targetRank}&pageSize=0"
        return getRanksForQuery(query, targetRank, start, pageSize)
    }

    def getRanksForQuery(query, targetRank, start, pageSize) {
        def results = [taxa: [], query: query]
        def url = grailsApplication.config.biocache.baseURL + "/ws/occurrences/search.json?" + query
        //println url
        def conn = new URL(url).openConnection()
        try {
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(50000)
            def json = conn.content.text
            //println json
            def facets = JSON.parse(json).facetResults
            def facet = facets.find { it.fieldName == (targetRank == 'order' ? 'bioOrder' : targetRank) }
            def facetValues = facet.fieldResult
            results.total = facetValues.size()
            // TODO: paginate - first 10 for now
            facetValues.sort { it.label }
            // paginate
            def first = Math.min(start, facetValues.size() - 1)
            def last = Math.min(first + pageSize, facetValues.size())
            def ranks = facetValues[first..last - 1]
            //println "${new Date().getTime() - timer}ms - parsed target taxa"
            // dummy family
            //results.taxa << bieLookup('Lamnidae', family)
            ranks.each {
                results.taxa << bieLookup(it.label, targetRank)
            }
        } catch (SocketTimeoutException e) {
            log.error("Timed out searching. URL= \${url}.", e)
        } catch (Exception e) {
            log.error("Failed search. ${e.getClass()} ${e.getMessage()} URL= ${url}.", e)
        }
        return results
    }

    def bieLookup(name, rank) {
        def resp = webService.getJson(ConfigurationHolder.config.bie.baseURL + "/species/" + name + ".json")
        def image = extractBestImage(resp.images, rank, name)
        def details = [name  : name, guid: resp.taxonConcept.guid,
                       common: bieService.extractBestCommonName(resp.commonNames),
                       image : image]
        return details
    }

    def extractBestImage(images, rank, name) {
        if (!images || images.size() == 0) {
            // if none found look at child species
            if (rank != 'species') {
                //println "${new Date().getTime() - timer}ms - searching children"
                def sppUrl = ConfigurationHolder.config.bie.baseURL + "/search.json?q=*&pageSize=1" +
                        "&fq=rank:species&fq=hasImage:true" +
                        "&fq=${rank == 'order' ? 'bioOrder' : rank}:${name}"
                //println sppUrl
                def sppConn = new URL(sppUrl).openConnection()
                try {
                    sppConn.setConnectTimeout(10000)
                    def result = JSON.parse(sppConn.content.text).searchResults?.results
                    //println "${new Date().getTime() - timer}ms - received and parsed children"
                    //println "${result.size()} spp returned for ${name}"
                    if (result) {
                        // just take the first one
                        return [repoLocation: ConfigurationHolder.config.bie.baseURL + "/repo" + result[0].image[9..-1],
                                thumbnail   : ConfigurationHolder.config.bie.baseURL + "/repo" + result[0].thumbnail[9..-1],
                                rights      : null]
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                }
            }
        } else {
            def preferred = images.findAll { it.preferred }
            // return first preferred name
            if (preferred) {
                return [repoLocation: preferred[0].repoLocation, thumbnail: preferred[0].thumbnail, rights: preferred[0].rights]
            }
            // else return first name
            return [repoLocation: images[0].repoLocation, thumbnail: images[0].thumbnail, rights: images[0].rights]
        }
        return [:]
    }

    def listMembers(list, targetRank) {
        //println "${new Date().getTime() - timer}ms"
        // currently need to make a request for each taxon in list
        def results = [taxa: []]  // map holding the list to build
        list.each { taxon ->
            //println "${new Date().getTime() - timer}ms - request target taxa"
            // TODO: only getting first 10 for now
            def url = ConfigurationHolder.config.bie.baseURL + "/search.json?q=*&pageSize=10" +
                    "&fq=rank:${targetRank}" +
                    "&fq=${taxon.rank == 'order' ? 'bioOrder' : taxon.rank}:${taxon.name}"
            def conn = new URL(url).openConnection()
            try {
                conn.setConnectTimeout(10000)
                conn.setReadTimeout(50000)
                def json = conn.content.text
                //println "${new Date().getTime() - timer}ms - received target taxa"
                def resp = JSON.parse(json).searchResults.results
                results.total = resp.size()
                //println "${new Date().getTime() - timer}ms - parsed target taxa"
                resp.each {
                    results.taxa << [name : it.name, guid: it.guid, common: it.commonNameSingle,
                                     image: urlForBestImage(it.guid, targetRank, it.name)]
                }
            } catch (SocketTimeoutException e) {
                log.warn "Timed out looking up taxon breakdown. URL= ${url}."
            } catch (Exception e) {
                log.warn "Failed to lookup taxon breakdown. ${e.getClass()} ${e.getMessage()} URL= ${url}."
            }
        }
        return results
    }

    /**
     * Find the best image for the specified taxon. Search child taxa if necessary.
     *
     * @param guid of the taxon
     * @param rank of the taxon
     * @param name of the taxon
     * @return image properties or null if none found
     */
    def urlForBestImage(guid, rank, name) {
        //println "${new Date().getTime() - timer}ms - find best image for ${rank}:${name}"
        if (!guid) {
            return ""
        }
        // get more info for taxon via guid
        def url = ConfigurationHolder.config.bie.baseURL + "/species/moreInfo/${guid}.json"
        //println url
        def conn = new URL(url).openConnection()
        try {
            conn.setConnectTimeout(10000)
            conn.setReadTimeout(50000)
            //println "${new Date().getTime() - timer}ms - more info received"
            def json = conn.content.text
            def imageList = JSON.parse(json).images
            // debug
/*
            println guid
            imageList.each {
                println "${it.preferred} - ${it.ranking} ${it.thumbnail ?: ''} ${it.repoLocation ? ' has raw' : ''}"
            }
*/
            // look for preferred image first
            def image = imageList.find { it.preferred && !it.isBlackListed }
            // else take the first image
            if (!image && imageList) {
                image = imageList[0]
            }

            // if none found look at child species
            if (!image && rank != 'species') {
                //println "${new Date().getTime() - timer}ms - searching children"
                def sppUrl = ConfigurationHolder.config.bie.baseURL + "/search.json?q=*&pageSize=1" +
                        "&fq=rank:species&fq=hasImage:true" +
                        "&fq=${rank == 'order' ? 'bioOrder' : rank}:${name}"
                //println sppUrl
                def sppConn = new URL(sppUrl).openConnection()
                try {
                    conn.setConnectTimeout(10000)
                    def result = JSON.parse(sppConn.content.text).searchResults?.results
                    //println "${new Date().getTime() - timer}ms - received and parsed children"
                    //println "${result.size()} spp returned for ${name}"
                    if (result) {
                        // just take the first one
                        image = [repoLocation: ConfigurationHolder.config.bie.baseURL + "/repo" + result[0].image[9..-1],
                                 thumbnail   : ConfigurationHolder.config.bie.baseURL + "/repo" + result[0].thumbnail[9..-1],
                                 rights      : null]
                        //println image
                    }
                } catch (Exception e) {
                    log.error(e.getMessage(), e)
                }
            }

            //println "${new Date().getTime() - timer}ms - done searching children"
            if (image) {
                return [repoLocation: image.repoLocation, thumbnail: image.thumbnail, rights: image.rights]
            } else {
                return null
            }
        } catch (SocketTimeoutException e) {
            log.error("Timed out looking up taxon image. URL= ${url}.", e)
        } catch (Exception e) {
            log.error("Failed to lookup taxon image. ${e.getClass()} ${e.getMessage()} URL= ${url}.", e)
        }
    }

    def buildFamilyHierarchy(list) {
        def results = []

        // get unique families
        def families = list.families
        def speciesRecords = list.results
        //println "size = " + families.size()

        //def findTime = 0, uniqueTime = 0, processTime = 0

        // try gathering records by family in one pass rather than searching separately for each
        // 100x faster for large results sets
        def recordsByFamily = [:]
        //long startTime = System.currentTimeMillis()
        speciesRecords.each {
            if (!recordsByFamily.containsKey(it.family)) {
                recordsByFamily.put it.family, []
            }
            recordsByFamily[it.family] << it
        }
        //println "collecting records by family: ${System.currentTimeMillis() - startTime}"

        // for each family
        families.each { name, data ->
//            println "Family: " + name
            //startTime = System.currentTimeMillis()
            def genera = []
            def genusRecords = recordsByFamily[name] //speciesRecords.findAll {it.family == name}
            //findTime += System.currentTimeMillis() - startTime

            // find unique genera
            def genusNames = []
            //startTime = System.currentTimeMillis()
            genusRecords.each {
                if (!genusNames.contains(it.genus)) {
                    genusNames << it.genus
                }
            }
            //uniqueTime += System.currentTimeMillis() - startTime

            // for each genus
            //startTime = System.currentTimeMillis()
            genusNames.each { genusName ->
                def species = genusRecords.findAll { it.genus == genusName }
                genera << [name      : genusName, speciesCount: species.size(), guid: species[0].genusGuid,
                           repSppGuid: bieService.pickFirstBestImage(species)?.guid]
            }
            //processTime += System.currentTimeMillis() - startTime
            results << [name    : name, guid: data.guid, common: data.common, image: data.image,
                        caabCode: data.caabCode, genera: genera.sort { it.name }]
        }

        //println "finding records: ${findTime} - unique genera: ${uniqueTime} - processing: ${processTime}"
        return results
    }
}
