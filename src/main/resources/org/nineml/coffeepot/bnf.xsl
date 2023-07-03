<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                exclude-result-prefixes="xs"
                version="3.0">

<xsl:output method="text" encoding="utf-8"/>

<xsl:template match="/ixml" priority="10">
  <xsl:variable name="bad-rules" as="xs:string*">
    <xsl:apply-templates select="rule"/>
  </xsl:variable>
  <xsl:if test="exists($bad-rules)">
    <xsl:value-of select="distinct-values($bad-rules)" separator=", "/>
  </xsl:if>
</xsl:template>

<xsl:template match="/*">
  <xsl:text>not a grammar</xsl:text>
</xsl:template>

<xsl:template match="rule">
  <xsl:if test=".//alts|.//repeat0|.//repeat1">
    <xsl:sequence select="@name/string()"/>
  </xsl:if>
</xsl:template>

</xsl:stylesheet>
