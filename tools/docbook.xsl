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
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:import href="https://cdn.docbook.org/release/xsltng/current/xslt/docbook.xsl"/>
<!--
<xsl:import href="/Users/ndw/Projects/docbook/xslTNG/build/xslt/docbook.xsl"/>
-->

<xsl:param name="persistent-toc" select="'true'"/>

<xsl:param name="section-toc-depth" select="1"/>

<xsl:param name="resource-base-uri" select="'/'"/>

<xsl:param name="css-links"
           select="'css/docbook.css css/docbook-screen.css css/coffeepot.css'"/>

<xsl:variable name="v:user-title-properties" as="element()*">
  <title xpath="self::db:chapter"
         label="false"/>
  <title xpath="self::db:section"
         label="false"/>
</xsl:variable>

<xsl:param name="chunk-section-depth" select="0"/>
<xsl:param name="chunk-include" as="xs:string*"
           select="('parent::db:book')"/>

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
