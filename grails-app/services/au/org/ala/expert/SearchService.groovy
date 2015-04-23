package au.org.ala.expert

class SearchService {

    def grailsApplication

    def buildQuery(SearchCommand cmd) {
        def criteria = ['dataResourceUid=' + grailsApplication.config.distribution.maps.dataResourceUid]
        switch (cmd.locationBasedOn) {
            case 'wkt': criteria << "wkt=" + cmd.wkt; break
            case ['circle','locality']:
                def circ = cmd.circle
                criteria << "lat=" + circ.lat
                criteria << "lon=" + circ.lng
                criteria << "radius=" + circ.radius
                break
            case 'marine area':
                criteria << "fid=" + cmd.myLayer
                criteria << "objectName=" + cmd.getMarineArea().imcra
        }

        if (cmd.depthBasedOn != 'all') {
            def dep = cmd.depthRange
            if (dep.minD) {
                criteria << "min_depth=" + dep.minD
            }
            if (dep.maxD) {
                criteria << "max_depth=" + dep.maxD
            }
        }

        if (cmd.groupBasedOn == 'fishGroup') {
            criteria << "groupName=" + formatGroupName(cmd.getGroup())
        }

        if (cmd.ecosystem) {
            criteria.addAll getEcosystemQuery(cmd.ecosystem)
        }

        if (cmd.families) {
            cmd.families.each {
                criteria << "family=" + it
            }
        }

        if (cmd.endemic) {
            criteria << "endemic=true"
        }

        return criteria.join('&')
    }

    def formatGroupName(name) {
        // replace '&' with encoding
        return name.encodeAsURL()
    }

    def search(SearchCommand cmd) {
        println "location based on ${cmd.locationBasedOn}"
        println "Radius = ${cmd.radius}"

        //cmd.families.each { log.debug it }
        //println "Families = ${cmd.families}"

        def results = []
        def query = buildQuery(cmd)

        log.debug "Query = " + query

        def servicePath = '/distributions'
        if (cmd.locationBasedOn == 'circle' || cmd.locationBasedOn == 'locality') {
            servicePath += '/radius'
        }

        try {
            def map = [:]
            withHttp(uri: grailsApplication.config.layers.service.baseURL) {
                def json = post(path: servicePath, body: query)
                //println json
                json.each {
                    //group by scientific name
                    def list = map.get(it.scientific)
                    if (list == null) {
                        list = []
                    }
                    list << it
                    map.put(it.scientific, list)
                }
            }
            //translate grouped records into a list
            //list will use first available non-grouped value when available
            map.each { k, it ->
                results << [
                        name            : first(it.collect { v -> v.scientific }),
                        common          : first(it.collect { v -> v.common_nam }),
                        caabCode        : first(it.collect { v -> v.caab_species_number }),
                        guid            : first(it.collect { v -> v.lsid }),
                        family          : first(it.collect { v -> v.family }),
                        familyGuid      : first(it.collect { v -> v.family_lsid }),
                        familyCaabCode  : first(it.collect { v -> v.caab_family_number }),
                        genus           : first(it.collect { v -> v.genus_name }),
                        genusGuid       : first(it.collect { v -> v.genus_lsid }),
                        specific        : first(it.collect { v -> v.specific_n }),
                        group           : first(it.collect { v -> v.group_name }),
                        primaryEcosystem: (first(it.collect { v -> v.pelagic_fl }) > 0 ? "p" : "") +
                                (first(it.collect { v -> v.coastal_fl }) ? "c" : "") +
                                (first(it.collect { v -> v.estuarine_fl }) ? "e" : "") +
                                (first(it.collect { v -> v.desmersal_fl }) ? "d" : ""),

                        areas           : it.collect { v ->
                            [areaName    : v.area_name,
                             metadataUrl : v.metadata_u,
                             spcode      : v.spcode,
                             gidx        : v.geom_idx,
                             authority   : v.authority_,
                             imageQuality: v.image_quality,
                             wmsurl      : v.wmsurl,
                             minDepth    : v.min_depth,
                             maxDepth    : v.max_depth,
                             endemic     : v.endemic
                            ]
                        }

                ]
            }
        } catch (Exception e) {
            return [error: "Spatial search: " + e.message, results: [], query: query]
        }

        log.info "results = ${results}"

        return [results: results, query: query/*, error: "spatial webservice not available"*/]
    }

    def first(list) {
        def value = null
        list.each { v ->
            if (value == null && v != null && !String.valueOf(v).isEmpty() && !String.valueOf(v).equalsIgnoreCase("null")) {
                value = v
            }
        }

        value
    }

    def speciesListSummary(List species) {
        def summary = [total: species.size()]
        def clone = species.clone()
        def families = clone.unique { it.family }
        summary.familyCount = families.size()
        summary['families'] = families.collect { it.family }
        return summary
    }
    
    List getEcosystemQuery(system) {
        switch (system) {
            case 'estuarine': return ['estuarine=true']
            // NOTE mis-spelling of demersal to match DB column name!
            case 'demersal': return ['desmersal=true']
            case 'pelagic': return ['pelagic=true']
            case 'coastal': return ['coastal=true']
            default: return [""]
        }
    }
}
