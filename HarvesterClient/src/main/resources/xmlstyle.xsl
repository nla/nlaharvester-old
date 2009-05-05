<?xml version="1.0" encoding="ISO-8859-1"?>



<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html"/>
	<xsl:template match="/">
				
	<div id="root">
		<xsl:apply-templates select="node()"/>
	</div>

	</xsl:template>

	<xsl:template match="*">
		
		<xsl:variable name="e_name">
			<xsl:value-of select="local-name()"/>
		</xsl:variable>
		&lt;<a name="{$e_name}" class="element" href="#" onClick="pick(this)"><span class="element"><xsl:value-of select="local-name()"/></span></a>
		<xsl:for-each select="@*" xml:space="preserve"> 
			<xsl:variable name="attr">
				<xsl:value-of select="local-name()"/>=&apos;<xsl:value-of select="."/>&apos;
			</xsl:variable>
			<a name="{$e_name}[{$attr}]" class="element" href="#" onClick="pick(this)"><span class="attribute_name"> <xsl:value-of select="local-name()"/></span>=&quot;<span class="attribute_value"><xsl:value-of select="."/></span>&quot;</a></xsl:for-each>&gt;<xsl:if test="not(*)"><xsl:value-of select="."/></xsl:if>	<xsl:if test="*"><div class="indent" id="{$e_name}"><xsl:apply-templates select="*"/></div></xsl:if>&lt;/<span class="element"><xsl:value-of select="local-name()"/></span>&gt;<br />
	</xsl:template>
</xsl:stylesheet>

