<?xml version="1.0" encoding="utf-8"?>
<xsl:stylesheet xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:cp="https://coffeepot.nineml.org/ns/functions"
                exclude-result-prefixes="#all"
                version="3.0">

<xsl:function name="cp:choose-alternative" as="map(*)">
  <xsl:param name="context" as="element()"/>
  <xsl:param name="options" as="map(*)"/>

  <xsl:variable name="count" select="($options?count,0)[1] + 1"/>

  <!-- no such choice! -->
  <xsl:variable name="choiceD"
                select="$context/children[symbol[@name='D']]/@id/string()"/>

  <xsl:sequence select="map{'selection': ($choiceD, $options?available-choices[1])[1],
                            'count': $count,
                            'ambiguous-choice': empty($choiceD) }"/>
</xsl:function>

</xsl:stylesheet>
