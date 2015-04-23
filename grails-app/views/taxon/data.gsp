<!DOCTYPE html>
<html>
<head>
    <title>Species data | ${grailsApplication.config.include.appName} | Atlas of Living Australia</title>
    <meta name="layout" content="main"/>
    <r:require modules="jquery, jqueryui, tooltipster, application, viewer"/>
</head>

<body class="species-data">
<header id="page-header">
    <div class="inner no-top">
        <g:if test="${grailsApplication.config.include.fish}">
            <hgroup>
                <h1 title="fishmap - find Australian marine fishes"></h1>
            </hgroup>
        </g:if>

        <g:if test="${!grailsApplication.config.include.fish}">
            <br/>

            <h1 title="${grailsApplication.config.include.appName} - Data explorer - species data">${grailsApplication.config.include.appName} - Data explorer - species data</h1>
            <br/>
        </g:if>
        <nav id="breadcrumb"><ol>
            <li><a href="${searchPage}">Search</a></li>
            <li class="last"><i>Results as data table</i></li></ol>
        </nav>
    </div>
</header>

<div class="inner">
    <h2 style="float:left;">Results for ${queryDescription ?: 'Australia'}</h2>

    <div id="controls">
        <g:if test="${grailsApplication.config.include.fish}">
            <label for="sortBy">Sort by:</label>
            <g:select
                    from="[[text: 'Family/genus/spp', id: 'taxa'], [text: 'Scientific name', id: 'name'], [text: 'Common name', id: 'common'], [text: 'CAAB code', id: 'caabCode']]"
                    name="sortBy" optionKey="id" optionValue="text"/>
        </g:if>
        <g:if test="${!grailsApplication.config.include.fish}">
            <label for="sortBy">Sort by:</label>
            <g:select
                    from="[[text: 'Family/genus/spp', id: 'taxa'], [text: 'Scientific name', id: 'name'], [text: 'Common name', id: 'common']]"
                    name="sortBy" optionKey="id" optionValue="text"/>
        </g:if>
        <label for="sortOrder">Sort order:</label>
        <g:select from="['normal', 'reverse']" name="sortOrder"/>
    </div>
    <table class="taxonData">
        <colgroup>
            <g:if test="${grailsApplication.config.include.fish}">
                <col id="tdCaabCode">
                <col id="tdFamily">
                <col id="tdSciName">
                <col id="tdCommon">
                <col id="tdGroup">
            </g:if>
            <g:if test="${!grailsApplication.config.include.fish}">
                <col id="tdFamily">
                <col id="tdSciName">
                <col id="tdCommon">
                <col id="tdName">
            </g:if>
        </colgroup>
        <thead>
        <tr>
            <g:if test="${grailsApplication.config.include.fish}">
                <th>CAAB Code</th>
                <th>Family</th>
                <th>Scientific name</th>
                <th>Common name</th>
                <th>Fish group</th>
                <th>Min depth</th>
                <th>Max depth</th>
                <th>Primary ecosystem</th>
            </g:if>
            <g:if test="${!grailsApplication.config.include.fish}">
                <th>Family</th>
                <th>Scientific name</th>
                <th>Common name</th>
                <th>Area name</th>
            </g:if>
        </thead>
        <tbody>
        <g:each in="${list}" var="it">
            <g:each in="${i.areas}" var="i">
                <tr>
                    <g:if test="${grailsApplication.config.include.fish}">
                        <!-- caab -->
                        <td>${it.caabCode}</td>
                        <!-- family -->
                        <td>${it.family}</td>
                        <!-- name -->
                        <td><em><a href="${grailsApplication.config.bie.baseURL}/species/${it.name}"
                                   title="Show ${rank} page">${it.name}</a></em></td>
                        <!-- common -->
                        <td>${it.common}</td>
                        <!-- group -->
                        <td>${it.group}</td>
                        <!-- min depth -->
                        <td>${i.minDepth}</td>
                        <!-- max depth -->
                        <td>${i.maxDepth}</td>
                        <!-- ecosystem -->
                        <td><tv:displayPrimaryEcosystem codes="${it.primaryEcosystem}"/></td>
                    </g:if>
                    <g:if test="${!grailsApplication.config.include.fish}">
                        <!-- family -->
                        <td>${it.family}</td>
                        <!-- name -->
                        <td><em><a href="${grailsApplication.config.bie.baseURL}/species/${it.name}"
                                   title="Show ${rank} page">${it.name}</a></em></td>
                        <!-- common -->
                        <td>${it.common}</td>
                        <!-- areaName -->
                        <td>${i.areaName}</td>
                    </g:if>
                </tr>
            </g:each>
        </g:each>
        </tbody>
    </table>
    <section id="pagination">
        <p id="viewLinks">
            Total <tv:pluraliseRank rank="species"/>: ${total}
            <g:link style="padding-left:20px;" action="view" params="[key: key]">Show all results by family</g:link>
            <g:link style="padding-left:20px;" action="species" params="[key: key]">Show all results by species</g:link>
        </p>
    </section>
</div>
<script type="text/javascript">
    $(document).ready(function () {
        tviewer.init("${grailsApplication.config.grails.serverURL}");
    });
</script>

</body>
</html>