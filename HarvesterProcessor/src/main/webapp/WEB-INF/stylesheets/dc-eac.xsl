<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:param name="day"/>
	<xsl:param name="month"/>
	<xsl:param name="year"/>

	<xsl:output method="xml"/>

	<xsl:template match="/">
		<xsl:for-each select="/record">
			<eac type="persname">
				<eacheader detaillevel="full" status="edited">
					<languagedecl>
						<xsl:for-each select="metadata/dc/language">
							<language><xsl:value-of select="."/></language>
						</xsl:for-each>
					</languagedecl>
					<xsl:for-each select="metadata/dc/identifier">
						<eacid><xsl:value-of select="."/></eacid>
					</xsl:for-each>

					<mainhist>
						<mainevent maintype="create">
							<maindate calendar="gregorian" normal="{$year}{$month}{$day}">
								<xsl:value-of select="$year"/><xsl:value-of select="$month"/><xsl:value-of select="$day"/>
							</maindate>
							<maindesc>
								Converted from DC by NLAHarvester
							</maindesc>
							<name>
								NLAHarvester
							</name>
						</mainevent>
					</mainhist>
					<srcdecl ownercode="" id="" system="" syskey="">
					</srcdecl>

				</eacheader>
				<condesc>
					<identity>
						<xsl:for-each select="metadata/dc/subject">
							<pershead>
								<xsl:value-of select="."/>
							</pershead>
						</xsl:for-each>
					</identity>
					<xsl:for-each select="metadata/dc/description">
						<desc>
							<xsl:value-of select="."/>
						</desc>
					</xsl:for-each>
					<resourcerel>
						<bibunit>
							<xsl:call-template name="bibunit">
								<xsl:with-param name="count" select="1"/>
							</xsl:call-template>
							<descnote>
								<xsl:for-each select="metadata/dc/type">
									<genreform>
										<xsl:value-of select="."/>
									</genreform>
								</xsl:for-each>
								<xsl:for-each select="metadata/dc/format">
									<genreform>
										<xsl:value-of select="."/>
									</genreform>
								</xsl:for-each>
							</descnote>
							<imprint>
								<xsl:for-each select="metadata/dc/date">
									<xsl:variable name="val"><xsl:value-of select="."/></xsl:variable>
									<date normal="{$val}">
										<xsl:value-of select="$val"/>
									</date>
								</xsl:for-each>
							</imprint>
							<xsl:for-each select="metadata/dc/title">
								<title><xsl:value-of select="."/></title>
							</xsl:for-each>
							<xsl:for-each select="metadata/dc/creator">
								<name><xsl:value-of select="."/></name>
							</xsl:for-each>
						</bibunit>
					</resourcerel>
				</condesc>
			</eac>
		</xsl:for-each>
	</xsl:template>

	<xsl:template name="bibunit">
		<xsl:param name="count" select="1"/>
		<xsl:if test="$count > 0">
			<xsl:text>.</xsl:text>
			<xsl:call-template name="dots">
				<xsl:with-param name="count" select="$count - 1"/>
			</xsl:call-template>
		</xsl:if>
	</xsl:template>

</xsl:stylesheet>




