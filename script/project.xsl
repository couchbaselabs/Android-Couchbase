<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform">

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="buildSpec">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
       <buildCommand>
           <name>org.eclipse.ui.externaltools.ExternalToolBuilder</name>
           <triggers>full,incremental,</triggers>
           <arguments>
                   <dictionary>
                           <key>LaunchConfigHandle</key>
                           <value>&lt;project&gt;/.externalToolBuilders/CouchbaseBuilder.launch</value>
                   </dictionary>
           </arguments>
   	  </buildCommand>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>