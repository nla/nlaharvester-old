<?xml version="1.0" encoding="ISO-8859-1"?>

<xsl:stylesheet version="1.0" xmlns:xsl="http://www.w3.org/1999/XSL/Transform">
	<xsl:output method="html"/>
	<xsl:strip-space  elements="*"/>
	
	<xsl:template match="/">
		<div style="background-color:#EEFDF;border:1px solid black;">
		<xsl:for-each select="//*[not(*)]">
			<xsl:if test="text()!=''">
				<div style="background-color:#EEFDF;padding:4px;float:left;">
				    <span class="element">
    					<xsl:for-each select="ancestor::*">
    					    <xsl:value-of select="local-name()"/><xsl:text>.</xsl:text>
    					</xsl:for-each> 
    					<xsl:value-of select="local-name()"/>
                    </span>
                    
				    <xsl:for-each select="@*">
				        &#160;&#160;&#160;&#160; <!-- non breaking spaces -->
				        <span class="attribute_name">
				            <xsl:value-of select="local-name()"/>
			            </span>
			            
				        <xsl:text> = </xsl:text>
				        
				        <span class="attribute_value">
				            &quot;<xsl:value-of select="."/>&quot;
			            </span>
				    </xsl:for-each> 
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
