<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:db="http://docbook.org/ns/docbook"
                xmlns:f="http://docbook.org/ns/docbook/functions"
                xmlns:fp="http://docbook.org/ns/docbook/functions/private"
                xmlns:h="http://www.w3.org/1999/xhtml"
                xmlns:m="http://docbook.org/ns/docbook/modes"
                xmlns:mp="http://docbook.org/ns/docbook/modes/private"
                xmlns:t="http://docbook.org/ns/docbook/templates"
                xmlns:tp="http://docbook.org/ns/docbook/templates/private"
                xmlns:v="http://docbook.org/ns/docbook/variables"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns="http://www.w3.org/1999/xhtml"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:import href="../website/docbook.xsl"/>

<!-- ============================================================ -->

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

<xsl:template match="*" mode="m:html-head-links">
  <xsl:next-match/>
  <link rel="shortcut icon" href="icon/CoffeePot.png"/>

  <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
  <link rel="stylesheet" href="css/nineml.css"/>
  <link rel="stylesheet" href="css/coffeepot.css"/>
</xsl:template>

<xsl:template match="db:option[not(@linkend)]" mode="m:docbook">
  <xsl:variable name="optid"
                select="if (contains(., ':'))
                        then replace(substring-before(., ':'), '--', '_')
                        else replace(., '--', '_')"/>

  <xsl:choose>
    <xsl:when test="id($optid) and ancestor::*[@xml:id=$optid]">
      <xsl:next-match/>
    </xsl:when>
    <xsl:when test="id($optid)">
      <a href="#{$optid}">
        <xsl:next-match/>
      </a>
    </xsl:when>
    <xsl:when test="$optid = '-jar' or $optid = '_help' or $optid = '_no-cache'
                    or $optid = '_tree-svg' or $optid = '_tree-xml'">
      <!-- I know these were removed... -->
      <xsl:next-match/>
    </xsl:when>
    <xsl:when test="$optid != ''">
      <xsl:message select="'Undocumented option: ' || $optid"/>
      <xsl:next-match/>
    </xsl:when>
    <xsl:otherwise>
      <xsl:next-match/>
    </xsl:otherwise>
  </xsl:choose>
</xsl:template>

</xsl:stylesheet>
