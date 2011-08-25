<?xml version="1.0" encoding="ISO-8859-1"?>
<xsl:stylesheet version="1.0"
xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
xmlns:android="http://schemas.android.com/apk/res/android">

<xsl:template match="@*|node()">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
  </xsl:copy>
</xsl:template>

<xsl:template match="application">
  <xsl:copy>
    <xsl:apply-templates select="@*|node()"/>
    <service android:name="com.couchbase.android.CouchbaseService" android:enabled="true" android:exported="false"></service>
  </xsl:copy>
</xsl:template>

</xsl:stylesheet>