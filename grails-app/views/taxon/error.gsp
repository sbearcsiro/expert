<!DOCTYPE html>
<html>
<head>
    <title>FishMap error</title>
    <meta name="layout" content="main"/>

</head>

<body class="family-list">
<header id="page-header">
    <div class="inner">
        <hgroup>
            <h1>${grailsApplication.config.include.appName} search - error page</h1>
        </hgroup>
        <nav id="breadcrumb"><ol>
            <li><a href="${searchPage}">Search</a></li>
            <li class="last"><i>Error</i></li></ol>
        </nav>
    </div>
</header>

<div class="inner">
    <h1>Results have timed out</h1>

    <p style="font-size:1.1em;">The search results you are trying to view no longer exist.
    Please repeat your <a href="${searchPage}">search</a>.</p>

    <p>The error is: ${message}.</p>
</div>
</body>
</html>