<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" 
    xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
    xmlns:marc="http://www.loc.gov/MARC21/slim">
    
	<xsl:output method="html"/>
	<xsl:strip-space  elements="*"/>
	
	<xsl:template match="marc:collection">
	    <div style="font-family: Courier, Courier New, monospace;">
            <xsl:apply-templates select="marc:record/*"/>
        </div>
	</xsl:template>
	
	<xsl:template match="marc:leader">
	    <xsl:text>000&#160;&#160;&#160;&#160;</xsl:text>
	    <xsl:value-of select="."/>
	    
	    <br />
	    <xsl:text>
        </xsl:text>
	</xsl:template>
	
	<xsl:template match="marc:controlfield">
	    <xsl:value-of select="@tag"/>
	    <xsl:text>&#160;&#160;&#160;&#160;</xsl:text>
	    <xsl:value-of select="."/><br />
	</xsl:template>
	
	<xsl:template match="marc:datafield">
	    <xsl:value-of select="@tag"/>
	    <xsl:text>&#160;</xsl:text>

        <xsl:apply-templates select="@ind1|@ind2"/>
	    
	    <xsl:text>&#160;</xsl:text>
	    
	    <xsl:apply-templates select="marc:subfield" />
	    
	    <br />
	    <xsl:text>
        </xsl:text>
        
	</xsl:template>
	
	<xsl:template match="marc:subfield">
	    <strong>
	        <xsl:text>$</xsl:text>
	        <xsl:value-of select="@code"/>
        </strong>
	    <xsl:value-of select="."/>
	</xsl:template>
	
	<xsl:template match="@ind1|@ind2">
	    <xsl:choose>
	        <xsl:when test="string-length(normalize-space()) = 0">
 	            <xsl:text>_</xsl:text>
 	        </xsl:when>
 	        <xsl:otherwise>
 	            <xsl:value-of select="."/>
 	        </xsl:otherwise>
 	    </xsl:choose>
 	</xsl:template>
	    
	
</xsl:stylesheet>
