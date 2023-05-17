<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:a="https://nineml.org/ns/describe-ambiguity"
                exclude-result-prefixes="#all"
                version="3.0">

<!-- This stylesheet simplifies the API XML for display -->

<xsl:output method="xml" encoding="utf-8" indent="yes"/>

<xsl:mode on-no-match="shallow-copy"/>

<xsl:template match="*">
  <xsl:element name="{local-name(.)}">
    <xsl:if test="@mark != '^'">
      <xsl:copy-of select="@mark"/>
    </xsl:if>
    <xsl:if test="local-name(.) != @name">
      <xsl:copy-of select="@name"/>
    </xsl:if>
    <xsl:if test="@from and @to">
      <xsl:attribute name="from" select="@from||'-'||@to"/>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>

<xsl:template match="a:nonterminal">
  <xsl:element name="{'_'||substring(@name,2)}">
    <xsl:if test="@mark != '^'">
      <xsl:copy-of select="@mark"/>
    </xsl:if>
    <xsl:apply-templates/>
  </xsl:element>
</xsl:template>

<xsl:template match="a:literal">
  <_>
    <xsl:if test="@mark != '^'">
      <xsl:copy-of select="@mark"/>
    </xsl:if>
    <xsl:apply-templates/>
  </_>
</xsl:template>

</xsl:stylesheet>
