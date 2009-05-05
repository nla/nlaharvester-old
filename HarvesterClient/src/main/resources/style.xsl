<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html"/>
	<xsl:template match="/">
		<div style="background-color:#EEFDF;border:1px solid black;">
		<xsl:for-each select="//*[not(*)]">
			<xsl:if test="text()!=''">
				<div style="background-color:#EEFDF;padding:4px;float:left;">
					<xsl:for-each select="ancestor::*"><xsl:value-of select="local-name()"/>.</xsl:for-each> 
				<xsl:value-of select="local-name()"/>
				</div>
				<div style="float:right;width:30%;background-color:#EEFDF;padding:4px">
					<xsl:value-of select="."/>
				</div>
				<div style="clear:both;"></div>
			</xsl:if>
		</xsl:for-each>
		</div>
	</xsl:template>
</xsl:stylesheet>

<!--				<xsl:for-each select="ancestor-or-self::*"><xsl:value-of select="local-name()"/><xsl:text></xsl:text>
					<xsl:for-each select="@*"><xsl:value-of select="local-name()"/>=&quot;<xsl:value-of select="."/>&quot;</xsl:for-each>.</xsl:for-each> -->

