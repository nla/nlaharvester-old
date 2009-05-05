<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="2.0" 
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	>
	<!--	xmlns:dc="http://purl.org/dc/elements/1.1/"
	xmlns:oai_dc="http://www.openarchives.org/OAI/2.0/oai_dc/"
	xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"-->

<xsl:output indent="yes" method="xml"/>

<xsl:template match="/">
	<xsl:copy>
		<xsl:apply-templates />
	</xsl:copy>
</xsl:template>

<xsl:template match="*">
	<xsl:element name="{local-name()}">
		<xsl:apply-templates select="@* | node()" />
	</xsl:element>
</xsl:template>

<xsl:template match="@*">
	<xsl:attribute name="{local-name()}">
		<xsl:value-of select="."/>
	</xsl:attribute>
</xsl:template>

<xsl:template match="text() | processing-instruction() | comment()">
	<xsl:copy />
</xsl:template>
<!--
<-->
</xsl:stylesheet>
