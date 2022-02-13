<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:mp="http://docbook.org/ns/docbook/modes/private"
                xmlns:rddl="http://www.rddl.org/"
                xmlns:t="http://docbook.org/ns/docbook/templates"
                xmlns:tp="http://docbook.org/ns/docbook/templates/private"
                xmlns:v="http://docbook.org/ns/docbook/variables"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:import href="https://cdn.docbook.org/release/xsltng/current/xslt/docbook.xsl"/>
<!--
<xsl:import href="/Users/ndw/Projects/docbook/xslTNG/build/xslt/docbook.xsl"/>
-->

<xsl:param name="lists-of-figures"  select="'false'"/>
<xsl:param name="lists-of-tables"   select="'false'"/>
<xsl:param name="lists-of-examples" select="'false'"/>
<xsl:param name="lists-of-equations" select="'false'"/>
<xsl:param name="lists-of-procedures" select="'false'"/>

<xsl:param name="persistent-toc" select="'true'"/>

<xsl:param name="section-toc-depth" select="1"/>

<xsl:param name="resource-base-uri" select="'/'"/>

<xsl:param name="css-links"
           select="'css/docbook.css css/docbook-screen.css css/coffeepot.css'"/>

<xsl:param name="chunk-section-depth" select="0"/>
<xsl:param name="chunk-include" as="xs:string*"
           select="('parent::db:book')"/>

<xsl:variable name="v:user-title-properties" as="element()*">
  <title xpath="self::db:chapter"
         label="false"/>
</xsl:variable>

<!-- ============================================================ -->

<xsl:variable name="v:templates" as="document-node()"
              xmlns:tmp="http://docbook.org/ns/docbook/templates"
              xmlns:db="http://docbook.org/ns/docbook"
              xmlns="http://www.w3.org/1999/xhtml">
  <xsl:document>
    <db:book>
      <header>
        <tmp:apply-templates select="db:title">
          <h1><tmp:content/></h1>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:subtitle">
          <h2><tmp:content/></h2>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:author">
          <div class="author">
            <h3><tmp:content/></h3>
          </div>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:releaseinfo">
          <p class="releaseinfo">
            <tmp:content/>
          </p>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:pubdate">
          <p class="pubdate"><tmp:content/></p>
        </tmp:apply-templates>
        <tmp:apply-templates select="db:legalnotice"/>
        <tmp:apply-templates select="db:abstract"/>
        <tmp:apply-templates select="db:revhistory"/>
        <tmp:apply-templates select="db:copyright"/>
        <tmp:apply-templates select="db:productname"/>
      </header>
    </db:book>
  </xsl:document>
</xsl:variable>

<xsl:template match="db:productname" mode="m:titlepage"
              expand-text="yes">
  <div class="versions">
    <p class="app">CoffeePot version {../db:productnumber/string()}</p>
    <p class="lib">
      <xsl:text>(Based on CoffeeGrinder </xsl:text>
      <xsl:sequence select="../db:bibliomisc[@role='coffeegrinder']/string()"/>
      <xsl:text> and CoffeeFilter </xsl:text>
      <xsl:sequence select="../db:bibliomisc[@role='coffeefilter']/string()"/>
      <xsl:text>.)</xsl:text>
    </p>
  </div>
</xsl:template>

<!-- ============================================================ -->

<xsl:template match="rddl:resource" mode="m:docbook">
  <rddl:resource>
    <xsl:sequence select="@*"/>
    <xsl:apply-templates select="node()" mode="m:docbook"/>
  </rddl:resource>
</xsl:template>

<xsl:template match="*" mode="m:html-head-links">
  <link rel="icon" href="/img/xr.png"/>
</xsl:template>

</xsl:stylesheet>
