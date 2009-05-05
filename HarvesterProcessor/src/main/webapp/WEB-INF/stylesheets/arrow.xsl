<?xml version="1.0" encoding="ISO-8859-1"?>

<!--

This only works on xml with no namespaces. Use strip-namespace.xsl first to remove them.

-->

<xsl:stylesheet version="2.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:output indent="yes" method="xml"/>


<xsl:template match="/">
		<xsl:for-each select="/record">
			<doc>
				<field name="oaiid"><xsl:value-of select="header/identifier"/></field>
				<field name="oaidatestamp"><xsl:value-of select="header/datestamp"/></field>
				<xsl:for-each select="metadata/dc">

					<!-- Get the primary creator to sort by -->					
					<xsl:if test="creator != ''">
						<field name="primary_creator">
							<xsl:value-of select="normalize-space(creator)"/>
						</field>
					</xsl:if>

					<!-- Get the primary subject to sort by -->					
					<xsl:if test="subject != ''">
						<field name="primary_subject">
							<xsl:value-of select="normalize-space(subject)"/>
						</field>
					</xsl:if>

					<!-- Get the primary type to sort by -->					
					<xsl:if test="type != ''">
						<field name="primary_type">
							<xsl:value-of select="normalize-space(type)"/>
						</field>
					</xsl:if>

					<!-- Get assorted fields -->
					<xsl:for-each select="title">
						<field name="title"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="creator">
						<field name="creator"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="subject">
						<field name="subject"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="description">
						<field name="description"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="publisher">
						<field name="publisher"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="type">
						<field name="type"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="format">
						<field name="format"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="identifier">
						<field name="identifier"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="source">
						<field name="source"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="language">
						<field name="language"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="relation">
						<field name="relation"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="coverage">
						<field name="coverage"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<xsl:for-each select="rights">
						<field name="rights"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

					<!-- Date requires some transformation -->
					<xsl:for-each select="date">
						<!-- sort them -->
						<field name="decade"><xsl:value-of select="normalize-space(.)"/></field>
						<field name="date"><xsl:value-of select="normalize-space(.)"/></field>
					</xsl:for-each>

				</xsl:for-each>
			</doc>
		</xsl:for-each>
</xsl:template>



</xsl:stylesheet>
