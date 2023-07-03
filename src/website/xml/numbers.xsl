<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:cp="https://coffeepot.nineml.org/ns/functions"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:function name="cp:choose-alternative" as="map(*)">
  <xsl:param name="context" as="element()"/>
  <xsl:param name="options" as="map(*)"/>

  <xsl:variable name="choice"
                select="$context/children[symbol[@name='decimal']]/@id"/>

  <xsl:sequence select="map { 'selection': $choice }"/>
</xsl:function>

</xsl:stylesheet>
