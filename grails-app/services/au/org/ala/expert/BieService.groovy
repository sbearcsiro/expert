package au.org.ala.expert

import grails.converters.JSON

class BieService {

    def webService, resultsService, grailsApplication

    /**
     * Returns a map of the unique families in the species list with relevant family metadata.
     *
     * @param list of species
     * @return map of families
     */
    def getFamilyMetadata(list/*, startTime*/) {

        // map to hold family metadata
        def families = [:]

        // get unique families + known metadata
        list.each {
            if (!families.containsKey(it.family)) {
                families.put it.family, [
                        guid: it.familyGuid,
                        caabCode: it.familyCaabCode
                ]
            }
        }

        // find the first species in the results set for this family with the highest rated image
        families.each { name, fam ->
            def spp = list.findAll { it.family == name }
            def repSpp = pickFirstBestImage(spp)
            fam.repSpeciesGuid = repSpp?.guid
            //println "Image for species ${repSpp.name} will be used for family ${name}"
        }

        //println "family loop finished at ----- " + (System.currentTimeMillis() - startTime) / 1000 + " seconds"

        // bulk lookup by guid for families
        def famBieData = doBulkLookup(families.values().collect {it.guid})
        def sppBieData = doBulkLookup(families.values().collect {it.repSpeciesGuid})
        families.each { name, fam ->
            def famData = famBieData[fam.guid]
            if (famData) {
                fam.common = famData.common
            }
            else {
                log.debug "no common name found for ${name}"
            }
            def sppData = sppBieData[fam.repSpeciesGuid]
            if (sppData && sppData.image && sppData.image.largeImageUrl?.toString() != "null" &&
                        (sppData.image.imageSource == grailsApplication.config.image.source.dataResourceUid ||
                        grailsApplication.config.image.source.dataResourceUid.isEmpty())) {
                fam.image = sppData.image
            }
            else {
                log.debug "no image found for ${name}"
            }
        }

        return families
    }

    def doBulkLookup(guids) {
        println (guids as JSON).toString()
        def data = webService.doJsonPost(grailsApplication.config.bie.services.baseURL,
                "species/guids/bulklookup.json", "", (guids as JSON).toString())
        Map results = [:]
        data.searchDTOList.each {item ->
                if (item != null) {
                    results.put item.guid, [
                            common: item.commonNameSingle,
                            image: [largeImageUrl   : item.largeImageUrl,
                                    smallImageUrl   : item.smallImageUrl,
                                    thumbnailUrl    : item.thumbnailUrl,
                                    imageMetadataUrl: item.imageMetadataUrl,
                                    imageSource     : item.imageSource]]
                }
            }

        return results
    }

    def listMissingImages(list) {
        def matchedWithMissingImage = []
        def unmatched = []
        def buckets = 0..(Math.ceil((list.size() as int)/1000) - 1)
        buckets.each { i ->
            def upper = Math.min(999 + i*1000, list.size() - 1)
            log.debug "processing records ${i*1000} to ${upper}"
            def guids = list[i*1000..upper].collect {it.guid}
            def res = doBulkLookup(guids)

            // find guids that did not have a bie match
            guids.each { guid ->
                if (!res.containsKey(guid)) {
                    unmatched << [guid: guid, name: list.find({it.guid == guid}).name]
                }
            }

            // how many matched species have no image
            res.each { guid, rec ->
                if (!rec.image?.largeImageUrl ||
                        (rec.image.imageSource != grailsApplication.config.image.source.dataResourceUid &&
                                !grailsApplication.config.image.source.dataResourceUid.isEmpty())) {
                    matchedWithMissingImage << [guid: guid, common: rec.common, name: list.find({it.guid == guid}).name]
                }
            }
        }
        log.debug "${matchedWithMissingImage.size()} matched species have no image"
        log.debug "${unmatched.size()} guids could not be matched in the BIE"

        matchedWithMissingImage.sort {it.name}

        return [matchedWithMissingImage: matchedWithMissingImage, unmatched: unmatched]
    }

    def pickFirstBestImage(list) {
        // use for loop so we can bail early as possible
        for (Map s in list) {
            if (s.imageQuality == 'E') {
                return s
            }
        }
        for (Map s in list) {
            if (s.imageQuality == 'G') {
                return s
            }
        }
        for (Map s in list) {
            if (s.imageQuality == 'A') {
                return s
            }
        }
        for (Map s in list) {
            if (s.imageQuality == 'P') {
                return s
            }
        }
        return list ? list[0] : null
    }

    def injectGenusMetadata(list) {

        // build a list of genus guids to lookup
        def guids = []
        list.each { fam ->
            fam.genera.each { gen ->
                if (gen.guid) {
                    guids << gen.guid
                }
                if (gen.repSppGuid) {
                    guids << gen.repSppGuid
                }
            }
        }

        // look up the metadata
        def md = betterBulkLookup(guids)

        // inject the metadata
        list.each { fam ->
            fam.genera.each { gen ->
                def genData = md[gen.guid]
                if (genData) {
                    gen.common = genData.common
                }
                else {
                    log.debug "No metadata found for genus ${gen.name} (guid = ${gen.guid})"
                }
                def sppData = md[gen.repSppGuid]
                if (sppData) {
                    if (sppData.image && sppData.image.largeImageUrl?.toString() != "null" &&
                            (sppData.image.imageSource == grailsApplication.config.image.source.dataResourceUid ||
                                    grailsApplication.config.image.source.dataResourceUid.isEmpty())) {
                        gen.image = sppData.image
                    }
                }
                else {
                    log.debug "No image found for genus ${gen.name} (guid = ${gen.guid})"
                }
            }
        }

        return list
    }

    def injectSpeciesMetadata(list) {

        // build a list of guids to lookup
        def guids = []
        list.each { sp ->
            if (sp.guid) {
                guids << sp.guid
            }
        }

        // look up the metadata
        def md = betterBulkLookup(guids)

        // inject the metadata
        list.each { sp ->
            def data = md[sp.guid]
            if (data) {
                //sp.common = data.common  // don't override common name with name from bie as CMAR is more authoritative
                if (data.image && data.image.largeImageUrl?.toString() != "null" &&
                        (data.image.imageSource == grailsApplication.config.image.source.dataResourceUid ||
                                grailsApplication.config.image.source.dataResourceUid.isEmpty())) {
                    sp.image = data.image
                }
            }
            else {
                log.debug "No metadata found for species ${sp.name} (guid = ${sp.guid})"
            }
        }

        return list
    }

    def betterBulkLookup(list) {
        def url = grailsApplication.config.bie.baseURL + "/species/guids/bulklookup.json"
        def data = webService.doPost(url, "", (list as JSON).toString())
        Map results = [:]
        data.resp.searchDTOList.each {item ->
            if (item != null && !item.toString().equals("null")) {
                results.put item.guid, [
                        common: item.commonNameSingle,
                        image: [largeImageUrl   : item.largeImageUrl,
                                smallImageUrl   : item.smallImageUrl,
                                thumbnailUrl    : item.thumbnailUrl,
                                imageMetadataUrl: item.imageMetadataUrl,
                                imageSource     : item.imageSource]]
            }
        }
        return results
    }

    static bieNameGuidCache = [:]  // temp cache while services are made more efficient

    def getBieMetadata(name, guid) {
        // use guid if known
        def key = guid ?: name

        // check cache first
        if (bieNameGuidCache[name]) {
            return bieNameGuidCache[name]
        }
        def resp = getJson(grailsApplication.config.bie.baseURL + "/species/" + key + ".json")
        if (!resp || resp.error) {
            return [name: name, guid: guid]
        }
        def details = [name: resp?.taxonConcept?.nameString ?: name, guid: resp?.taxonConcept?.guid,
                       common: extractBestCommonName(resp?.commonNames),
                       image: extractPreferredImage(resp?.images)]
        bieNameGuidCache[name] = details
        return details
    }

    def getPreferredImage(name) {
        def resp = getJson(grailsApplication.config.bie.baseUrl + "/species/${name}.json")
        return extractPreferredImage(resp.images)
    }

    def extractPreferredImage(images) {
        if (images) {
            def preferred = images.findAll {it.preferred}
            // return first preferred name
            if (preferred) {
                return [repoLocation: preferred[0].repoLocation, thumbnail: preferred[0].thumbnail, rights: preferred[0].rights]
            }
            // else return first image
            return [repoLocation: images[0].repoLocation, thumbnail: images[0].thumbnail, rights: images[0].rights]
        }
        return null
    }

    def extractBestCommonName(names) {
        if (names) {
            def preferred = names.findAll {it.preferred}
            // return first preferred name
            if (preferred) { return preferred[0].nameString}
            // else return first name
            return names[0].nameString
        }
        return ""
    }
}
