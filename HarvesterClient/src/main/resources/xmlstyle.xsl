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
	
		<xsl:text>&lt;</xsl:text>
		<a name="{$e_name}" class="element" href="#" onClick="pick(this)"><span class="element"><xsl:value-of select="local-name()"/></span></a>
	
		<xsl:for-each select="@*"> 
	    		    
			<xsl:variable name="attr">
				<xsl:value-of select="local-name()"/>
				<xsl:text>=&apos;</xsl:text>
				<xsl:value-of select="."/>
				<xsl:text>&apos;</xsl:text>
			</xsl:variable>
		
			<xsl:text>&#160;</xsl:text>
			<a name="{$e_name}[{$attr}]" class="element" href="#" onClick="pick(this)">
			    <span class="attribute_name"> 
			        <xsl:value-of select="local-name()"/>
			    </span>
			    <xsl:text>=&quot;</xsl:text>
			    <span class="attribute_value">
			        <xsl:value-of select="."/>
			    </span>
			    <xsl:text>&quot;</xsl:text>
			</a>
		
		</xsl:for-each>
		<xsl:text>&gt;</xsl:text>
	
		<xsl:for-each select="node()"> 
		    <xsl:choose>
		        <xsl:when test="self::*">
		            <xsl:choose>
		                <!-- only indent a node if its not preceded immediately by  text -->
    		            <xsl:when test="not(string-length(normalize-space(preceding-sibling::text())) > 0)">
        		            <div class="indent" id="{$e_name}">
        		                <xsl:apply-templates select="."/>
        		            </div>
    		            </xsl:when>
    		            
    		            <xsl:otherwise>
    		                 <xsl:apply-templates select="."/>
    		            </xsl:otherwise>
		            </xsl:choose>
		        </xsl:when>
		        
		        <xsl:otherwise> <!-- text nodes -->
		             <xsl:apply-templates select="."/>
		        </xsl:otherwise>
		        
	        </xsl:choose>
	    </xsl:for-each>
	    
		<xsl:text>&lt;/</xsl:text>
		<span class="element"><xsl:value-of select="local-name()"/></span>
		<xsl:text>&gt;</xsl:text>
		
		<!-- we only put a newline after a node if its not followed immediately by text -->
		<xsl:if test="not(string-length(normalize-space(following-sibling::text())) > 0)">
		    <br />
		</xsl:if>
		
	</xsl:template>
	
	<xsl:template match="text()">
	     <xsl:value-of select="."/>
	</xsl:template>
</xsl:stylesheet>

