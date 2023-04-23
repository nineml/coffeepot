<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:cp="https://coffeepot.nineml.org/ns/functions"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:function name="cp:choose-alternative" as="xs:integer">
  <xsl:param name="alternatives" as="element()+"/>
  <xsl:sequence select="$alternatives[decimal]/@alternative"/>
</xsl:function>

</xsl:stylesheet>
