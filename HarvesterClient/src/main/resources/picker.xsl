<?xml version="1.0" encoding="ISO-8859-1"?>



<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html"/>
	<xsl:template match="/">

	<div id="root">
		<xsl:apply-templates select="node()"/>
	</div>
	</xsl:template>

	<xsl:template match="*">

		<xsl:variable name="base">
			<xsl:value-of select="local-name()"/>
		</xsl:variable>		
<xsl:variable name="e_name">
<xsl:value-of select="local-name()"/>
<xsl:if test="@*"><xsl:for-each select="@*">[@<xsl:value-of select="local-name()"/>=&apos;<xsl:value-of select="."/>&apos;]</xsl:for-each></xsl:if>
</xsl:variable>

		<a name="{$e_name}" class="element" href="#" onClick="pick(this)">
			&lt;<span class="element"><xsl:value-of select="local-name()"/></span>
		</a>

		<xsl:for-each select="@*"> 
			<xsl:text> </xsl:text>
			<xsl:variable name="attr">
				<xsl:value-of select="local-name()"/>
			</xsl:variable>
			<a name="{$base}/@{$attr}" class="element" href="#" onClick="pick(this)"><span class="attribute_name"> <xsl:value-of select="local-name()"/></span>=&quot;<span class="attribute_value"><xsl:value-of select="."/></span>&quot;</a></xsl:for-each>&gt;<br />
		
		<div class="indent" id="{$e_name}">
			<xsl:apply-templates select="*"/>
		</div>
	</xsl:template>
</xsl:stylesheet>

