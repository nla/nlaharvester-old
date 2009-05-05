<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:transform 
	               
	xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
	xmlns="http://jefferson.village.virginia.edu/eac"
	version="1.0">

	<xsl:output method="xml"/>


	<xsl:template match="record">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="metadata">
		<xsl:apply-templates/>
	</xsl:template>

	<xsl:template match="mapsIdentity">
		<eacIdentity>
			<xsl:apply-templates select="maps"/>
		</eacIdentity>
	</xsl:template>


	<xsl:template name="getType"></xsl:template>

	<xsl:template match="maps">
		<xsl:if test="mapsId/@source='ANL:MA' or mapsId/@source='destra Media' or ''=''">
			<eac type="persname">
				<eacheader status="edited">
					<xsl:apply-templates select="mapsId"/>

					<!-- Not actually sure how this is to be mapped
					<xsl:apply-templates select="altId"/> -->

					<!-- Not used in PEPO
					<xsl:apply-templates select="identityLink"/> -->

					<mainhist>
						<xsl:apply-templates select="recordInfo/recordCreator"/>
						<xsl:apply-templates select="recordInfo/recordChanger"/>
					</mainhist>
					<xsl:apply-templates select="recordInfo/languageOfCataloging/languageTerm"/>
					<xsl:apply-templates select="recordInfo/recordContentSource"/>
				</eacheader>
				<condesc>
					<identity>
						<xsl:apply-templates select="name[@type='personal']"/>
						<xsl:apply-templates select="name[@type='corporate']"/>
						<xsl:apply-templates select="name[@type='event']"/>
					</identity>
					<desc>
<!--						<persdesc>
						</persdesc>-->
						<xsl:apply-templates select="biogHist"/>
					</desc>
					
					<!-- doesn't seem to be used by maps
					<eacrels>
					</eacrels>
					-->

					<!-- these might be used for sourceInfo, but it's all a bit unclear
					<resourcerels>
					</resourcerels>
					-->

					<xsl:if test="fieldOfActivity|honours">
						<funactrels>
							<xsl:apply-templates select="fieldOfActivity"/>
							<xsl:apply-templates select="honours"/>
						</funactrels>
					</xsl:if>
				</condesc>
			</eac>
		</xsl:if>
	</xsl:template>



	<xsl:template match="honoursTerm">
		<funact>
			<xsl:value-of select="."/>
		</funact>
	</xsl:template>



	<xsl:template match="honoursCitation|placing">
		<descnote>
			<xsl:value-of select="."/>
		</descnote>
	</xsl:template>



	<xsl:template match="honours">
		<funactrel>
			<xsl:apply-templates select="honoursTerm"/>
			<xsl:apply-templates select="honoursCitation"/>
			<xsl:apply-templates select="placing"/>
			<xsl:apply-templates select="date"/>
		</funactrel>
	</xsl:template>



	<xsl:template match="recordChanger">
		<mainevent maintype="updated">
			<name>
				<xsl:value-of select="."/>
			</name>
			<xsl:variable name="maindate"><xsl:value-of select="../recordChangeDate"/></xsl:variable>
			<xsl:if test="$maindate!=''">
				<maindate calendar="gregorian" normal="{$maindate}">
					<xsl:value-of select="../recordChangeDate"/>
				</maindate>
			</xsl:if>
		</mainevent>
	</xsl:template>



	<xsl:template match="recordCreator">
		<mainevent maintype="create">
			<name>
				<xsl:value-of select="."/>
			</name>
			<xsl:variable name="maindate"><xsl:value-of select="../recordCreationDate"/></xsl:variable>
			<xsl:if test="$maindate!=''">
				<maindate calendar="gregorian" normal="{$maindate}">
					<xsl:value-of select="../recordCreationDate"/>
				</maindate>
			</xsl:if>
		</mainevent>
	</xsl:template>



	<xsl:template match="languageOfCataloging/languageTerm">
		<languagedecl>
			<language scriptcode="latin">
				<xsl:value-of select="."/>
			</language>
		</languagedecl>
	</xsl:template>



	<xsl:template match="recordContentSource">
		<sourcedecl>
			<xsl:variable name="oaiid"><xsl:value-of select="//header/identifier"/></xsl:variable>
			<source ownercode="oaiid" syskey="{$oaiid}"><xsl:value-of select="//header/identifier"/></source>
		</sourcedecl>
	</xsl:template>



	<xsl:template match="mapsId">
		<xsl:variable name="authority"><xsl:value-of select="@authority"/></xsl:variable>
		<xsl:variable name="source"><xsl:value-of select="@source"/></xsl:variable>
		<xsl:variable name="id"><xsl:value-of select="@id"/></xsl:variable>

		<eacid syskey="{$id}" ownercode="{$source}">
			<xsl:value-of select="$id"/>
		</eacid>
	</xsl:template>



	<xsl:template match="altId">
		<xsl:variable name="authority"><xsl:value-of select="@authority"/></xsl:variable>
		<xsl:variable name="source"><xsl:value-of select="@source"/></xsl:variable>
		<xsl:variable name="id"><xsl:value-of select="@id"/></xsl:variable>
		<xsl:variable name="type"><xsl:value-of select="@type"/></xsl:variable>
		<eacid syskey="{$authority}" type="{$type}" ownercode="{$source}">
			<xsl:value-of select="$id"/>
		</eacid>
	</xsl:template>



	<xsl:template match="identityLink">
		<xsl:variable name="authority"><xsl:value-of select="@authority"/></xsl:variable>
		<xsl:variable name="source"><xsl:value-of select="@source"/></xsl:variable>
		<xsl:variable name="id"><xsl:value-of select="@id"/></xsl:variable>
		<xsl:variable name="linktype"><xsl:value-of select="@linktype"/></xsl:variable>
		<eacid syskey="{$id}" type="{$linktype}" ownercode="{$source}">
			<xsl:value-of select="$id"/>
		</eacid>
	</xsl:template>


	<xsl:template match="name[@type='personal']">
		<pershead>
			<xsl:apply-templates/>
		</pershead>
	</xsl:template>

	<xsl:template match="name[@type='event']">
		<corphead>
			<xsl:apply-templates/>
		</corphead>
	</xsl:template>



	<xsl:template match="name[@type='corporate']">
		<corphead>
			<xsl:apply-templates/>
		</corphead>
	</xsl:template>



	<xsl:template match="namePart">
		<xsl:variable name="type"><xsl:value-of select="@type"/></xsl:variable>
		<xsl:if test="$type!=''">
			<part type="{$type}">
			<xsl:value-of select="."/>
			</part>
		</xsl:if>
		<xsl:if test="$type=''">
			<part>
			<xsl:value-of select="."/>
			</part>
		</xsl:if>
	</xsl:template>



	<xsl:template match="biogHist">
		<bioghist>
			<p>
				<xsl:value-of select="."/>
			</p>
		</bioghist>
	</xsl:template>



	<xsl:template match="fieldOfActivity">
			<funactrel>
				<xsl:apply-templates />
			</funactrel>
	</xsl:template>	



	<xsl:template match="date">
		<date>
			<xsl:value-of select="."/>
		</date>
	</xsl:template>


	<xsl:template match="activityTerm">
		<xsl:variable name="type"><xsl:value-of select="@type"/></xsl:variable>
		<xsl:variable name="authority"><xsl:value-of select="@authority"/></xsl:variable>
		<funact>
			<xsl:value-of select="."/>
		</funact>
	</xsl:template>


	<xsl:template match="*">
	</xsl:template>
</xsl:transform>






