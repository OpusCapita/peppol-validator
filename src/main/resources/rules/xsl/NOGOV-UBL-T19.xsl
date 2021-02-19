<xsl:stylesheet xmlns:xs="http://www.w3.org/2001/XMLSchema"
                xmlns:xsd="http://www.w3.org/2001/XMLSchema"
                xmlns:saxon="http://saxon.sf.net/"
                xmlns:xsl="http://www.w3.org/1999/XSL/Transform"
                xmlns:schold="http://www.ascc.net/xml/schematron"
                xmlns:iso="http://purl.oclc.org/dsdl/schematron"
                xmlns:xhtml="http://www.w3.org/1999/xhtml"
                xmlns:cbc="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
                xmlns:cac="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                xmlns:ubl="urn:oasis:names:specification:ubl:schema:xsd:Catalogue-2"
                version="2.0"><!--Implementers: please note that overriding process-prolog or process-root is 
    the preferred method for meta-stylesheets to use where possible. -->
   <xsl:param name="archiveDirParameter"/>
   <xsl:param name="archiveNameParameter"/>
   <xsl:param name="fileNameParameter"/>
   <xsl:param name="fileDirParameter"/>
   <xsl:variable name="document-uri">
      <xsl:value-of select="document-uri(/)"/>
   </xsl:variable>

   <!--PHASES-->


   <!--PROLOG-->
   <xsl:output xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
               method="xml"
               omit-xml-declaration="no"
               standalone="yes"
               indent="yes"/>

   <!--XSD TYPES FOR XSLT2-->


   <!--KEYS AND FUNCTIONS-->


   <!--DEFAULT RULES-->


   <!--MODE: SCHEMATRON-SELECT-FULL-PATH-->
   <!--This mode can be used to generate an ugly though full XPath for locators-->
   <xsl:template match="*" mode="schematron-select-full-path">
      <xsl:apply-templates select="." mode="schematron-get-full-path"/>
   </xsl:template>

   <!--MODE: SCHEMATRON-FULL-PATH-->
   <!--This mode can be used to generate an ugly though full XPath for locators-->
   <xsl:template match="*" mode="schematron-get-full-path">
      <xsl:apply-templates select="parent::*" mode="schematron-get-full-path"/>
      <xsl:text>/</xsl:text>
      <xsl:choose>
         <xsl:when test="namespace-uri()=''">
            <xsl:value-of select="name()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text>*:</xsl:text>
            <xsl:value-of select="local-name()"/>
            <xsl:text>[namespace-uri()='</xsl:text>
            <xsl:value-of select="namespace-uri()"/>
            <xsl:text>']</xsl:text>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:variable name="preceding"
                    select="count(preceding-sibling::*[local-name()=local-name(current())                                   and namespace-uri() = namespace-uri(current())])"/>
      <xsl:text>[</xsl:text>
      <xsl:value-of select="1+ $preceding"/>
      <xsl:text>]</xsl:text>
   </xsl:template>
   <xsl:template match="@*" mode="schematron-get-full-path">
      <xsl:apply-templates select="parent::*" mode="schematron-get-full-path"/>
      <xsl:text>/</xsl:text>
      <xsl:choose>
         <xsl:when test="namespace-uri()=''">@<xsl:value-of select="name()"/>
         </xsl:when>
         <xsl:otherwise>
            <xsl:text>@*[local-name()='</xsl:text>
            <xsl:value-of select="local-name()"/>
            <xsl:text>' and namespace-uri()='</xsl:text>
            <xsl:value-of select="namespace-uri()"/>
            <xsl:text>']</xsl:text>
         </xsl:otherwise>
      </xsl:choose>
   </xsl:template>

   <!--MODE: SCHEMATRON-FULL-PATH-2-->
   <!--This mode can be used to generate prefixed XPath for humans-->
   <xsl:template match="node() | @*" mode="schematron-get-full-path-2">
      <xsl:for-each select="ancestor-or-self::*">
         <xsl:text>/</xsl:text>
         <xsl:value-of select="name(.)"/>
         <xsl:if test="preceding-sibling::*[name(.)=name(current())]">
            <xsl:text>[</xsl:text>
            <xsl:value-of select="count(preceding-sibling::*[name(.)=name(current())])+1"/>
            <xsl:text>]</xsl:text>
         </xsl:if>
      </xsl:for-each>
      <xsl:if test="not(self::*)">
         <xsl:text/>/@<xsl:value-of select="name(.)"/>
      </xsl:if>
   </xsl:template>
   <!--MODE: SCHEMATRON-FULL-PATH-3-->
   <!--This mode can be used to generate prefixed XPath for humans 
	(Top-level element has index)-->
   <xsl:template match="node() | @*" mode="schematron-get-full-path-3">
      <xsl:for-each select="ancestor-or-self::*">
         <xsl:text>/</xsl:text>
         <xsl:value-of select="name(.)"/>
         <xsl:if test="parent::*">
            <xsl:text>[</xsl:text>
            <xsl:value-of select="count(preceding-sibling::*[name(.)=name(current())])+1"/>
            <xsl:text>]</xsl:text>
         </xsl:if>
      </xsl:for-each>
      <xsl:if test="not(self::*)">
         <xsl:text/>/@<xsl:value-of select="name(.)"/>
      </xsl:if>
   </xsl:template>

   <!--MODE: GENERATE-ID-FROM-PATH -->
   <xsl:template match="/" mode="generate-id-from-path"/>
   <xsl:template match="text()" mode="generate-id-from-path">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:value-of select="concat('.text-', 1+count(preceding-sibling::text()), '-')"/>
   </xsl:template>
   <xsl:template match="comment()" mode="generate-id-from-path">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:value-of select="concat('.comment-', 1+count(preceding-sibling::comment()), '-')"/>
   </xsl:template>
   <xsl:template match="processing-instruction()" mode="generate-id-from-path">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:value-of select="concat('.processing-instruction-', 1+count(preceding-sibling::processing-instruction()), '-')"/>
   </xsl:template>
   <xsl:template match="@*" mode="generate-id-from-path">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:value-of select="concat('.@', name())"/>
   </xsl:template>
   <xsl:template match="*" mode="generate-id-from-path" priority="-0.5">
      <xsl:apply-templates select="parent::*" mode="generate-id-from-path"/>
      <xsl:text>.</xsl:text>
      <xsl:value-of select="concat('.',name(),'-',1+count(preceding-sibling::*[name()=name(current())]),'-')"/>
   </xsl:template>

   <!--MODE: GENERATE-ID-2 -->
   <xsl:template match="/" mode="generate-id-2">U</xsl:template>
   <xsl:template match="*" mode="generate-id-2" priority="2">
      <xsl:text>U</xsl:text>
      <xsl:number level="multiple" count="*"/>
   </xsl:template>
   <xsl:template match="node()" mode="generate-id-2">
      <xsl:text>U.</xsl:text>
      <xsl:number level="multiple" count="*"/>
      <xsl:text>n</xsl:text>
      <xsl:number count="node()"/>
   </xsl:template>
   <xsl:template match="@*" mode="generate-id-2">
      <xsl:text>U.</xsl:text>
      <xsl:number level="multiple" count="*"/>
      <xsl:text>_</xsl:text>
      <xsl:value-of select="string-length(local-name(.))"/>
      <xsl:text>_</xsl:text>
      <xsl:value-of select="translate(name(),':','.')"/>
   </xsl:template>
   <!--Strip characters-->
   <xsl:template match="text()" priority="-1"/>

   <!--SCHEMA SETUP-->
   <xsl:template match="/">
      <svrl:schematron-output xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                              title="Norwegian rules for EHF Catalogue"
                              schemaVersion="iso">
         <xsl:comment>
            <xsl:value-of select="$archiveDirParameter"/>   
		 <xsl:value-of select="$archiveNameParameter"/>  
		 <xsl:value-of select="$fileNameParameter"/>  
		 <xsl:value-of select="$fileDirParameter"/>
         </xsl:comment>
         <svrl:ns-prefix-in-attribute-values uri="urn:oasis:names:specification:ubl:schema:xsd:CommonBasicComponents-2"
                                             prefix="cbc"/>
         <svrl:ns-prefix-in-attribute-values uri="urn:oasis:names:specification:ubl:schema:xsd:CommonAggregateComponents-2"
                                             prefix="cac"/>
         <svrl:ns-prefix-in-attribute-values uri="urn:oasis:names:specification:ubl:schema:xsd:Catalogue-2"
                                             prefix="ubl"/>
         <svrl:active-pattern>
            <xsl:attribute name="document">
               <xsl:value-of select="document-uri(/)"/>
            </xsl:attribute>
            <xsl:apply-templates/>
         </svrl:active-pattern>
         <xsl:apply-templates select="/" mode="M4"/>
      </svrl:schematron-output>
   </xsl:template>

   <!--SCHEMATRON PATTERNS-->
   <svrl:text xmlns:svrl="http://purl.oclc.org/dsdl/svrl">Norwegian rules for EHF Catalogue</svrl:text>

   <!--PATTERN -->


	  <!--RULE -->
   <xsl:template match="/ubl:Catalogue" priority="1011" mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl" context="/ubl:Catalogue"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cbc:ActionCode or count(cac:CatalogueLine/cbc:ActionCode) = count(cac:CatalogueLine)"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                                test="cbc:ActionCode or count(cac:CatalogueLine/cbc:ActionCode) = count(cac:CatalogueLine)">
               <xsl:attribute name="id">NOGOV-T19-R001</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R001]-A Catalogue must contain ActionCode on either Header or Line level</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="not(cac:ValidityPeriod/cbc:EndDate) or current-date() &lt;= xs:date(cac:ValidityPeriod/cbc:EndDate)"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                                test="not(cac:ValidityPeriod/cbc:EndDate) or current-date() &lt;= xs:date(cac:ValidityPeriod/cbc:EndDate)">
               <xsl:attribute name="id">NOGOV-T19-R002</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R002]-A Catalogue must have a validity period enddate grater or equal to the current date</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cac:ValidityPeriod"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cac:ValidityPeriod">
               <xsl:attribute name="id">NOGOV-T19-R008</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R008]-A cataloge MUST have a validity period.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cbc:VersionID"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cbc:VersionID">
               <xsl:attribute name="id">NOGOV-T19-R012</xsl:attribute>
               <xsl:attribute name="flag">warning</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R012]-A catalogue should have a catalogue version.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="/ubl:Catalogue/cac:ValidityPeriod" priority="1010" mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                       context="/ubl:Catalogue/cac:ValidityPeriod"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cbc:StartDate"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cbc:StartDate">
               <xsl:attribute name="id">NOGOV-T19-R009</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R009]-A catalogue MUST have a validity start date.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cac:ReceiverParty" priority="1009" mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl" context="cac:ReceiverParty"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cbc:EndpointID"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cbc:EndpointID">
               <xsl:attribute name="id">NOGOV-T19-R010</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R010]-A catalogue MUST have an endpoint ID for receiver.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cac:SellerSupplierParty/cac:Party" priority="1008" mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                       context="cac:SellerSupplierParty/cac:Party"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cbc:EndpointID"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cbc:EndpointID">
               <xsl:attribute name="id">NOGOV-T19-R013</xsl:attribute>
               <xsl:attribute name="flag">warning</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R013]-A catalogue should have an endpoint ID for seller.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cac:TaxScheme" priority="1007" mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl" context="cac:TaxScheme"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cbc:ID"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cbc:ID">
               <xsl:attribute name="id">NOGOV-T19-R011</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R011]-Every tax scheme MUST be defined through an identifier.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cac:PartyLegalEntity/cbc:CompanyID[@schemeID]"
                 priority="1006"
                 mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                       context="cac:PartyLegalEntity/cbc:CompanyID[@schemeID]"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="@schemeID = 'NO:ORGNR'"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="@schemeID = 'NO:ORGNR'">
               <xsl:attribute name="id">NOGOV-T19-R024</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R024]-CompanyID for legal entity qualifier must have value 'NO:ORGNR' when provided.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cac:CatalogueLine" priority="1005" mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl" context="cac:CatalogueLine"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cbc:OrderableIndicator"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cbc:OrderableIndicator">
               <xsl:attribute name="id">NOGOV-T19-R003</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R003]-A Catalogue line MUST have an orderable indicator</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cac:Item"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cac:Item">
               <xsl:attribute name="id">NOGOV-T19-R004</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R004]-A Catalogue line MUST have item/article information</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cac:CatalogueLine/cac:Item" priority="1004" mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                       context="cac:CatalogueLine/cac:Item"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="cbc:Name"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl" test="cbc:Name">
               <xsl:attribute name="id">NOGOV-T19-R005</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R005]-A Catalogue item MUST have a name</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cbc:ProfileID" priority="1003" mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl" context="cbc:ProfileID"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test=". = 'urn:www.cenbii.eu:profile:bii01:ver2.0'"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                                test=". = 'urn:www.cenbii.eu:profile:bii01:ver2.0'">
               <xsl:attribute name="id">EHFPROFILE-T19-R001</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[EHFPROFILE-T19-R001]-A catalogue must only be used in profile 1</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cbc:DocumentTypeCode[@listID='urn:gs1:gdd:cl:ReferencedFileTypeCode']"
                 priority="1002"
                 mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                       context="cbc:DocumentTypeCode[@listID='urn:gs1:gdd:cl:ReferencedFileTypeCode']"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="some $code in tokenize('360_DEGREE_IMAGE ASSEMBLY_INSTRUCTIONS AUDIO CERTIFICATION CHEMICAL_ASSESSMENT_SUMMARY CHEMICAL_SAFETY_REPORT CHILD_NUTRITION_LABEL CONSUMER_HANDLING_AND_STORAGE CROSSSECTION_VIEW DIET_CERTIFICATE DOCUMENT DOP_SHEET DRUG_FACT_LABEL ENERGY_LABEL GROUP_CHARACTERISTIC_SHEET HAZARDOUS_SUBSTANCES_DATA IFU INTERNAL_VIEW LOGO MARKETING_INFORMATION MATERIAL_SAMPLES MOBILE_DEVICE_IMAGE NUTRITION_FACT_LABEL ORGANIC_CERTIFICATE OTHER_EXTERNAL_INFORMATION OUT_OF_PACKAGE_IMAGE PACKAGING_ARTWORK PLANOGRAM PRODUCT_FORMULATION_STATEMENT PRODUCT_IMAGE PRODUCT_LABEL_IMAGE PRODUCT_WEBSITE QR_CODE QUALITY_CONTROL_PLAN RECIPE_WEBSITE REGULATORY_INSPECTION_AUDIT RISK_ANALYSIS_DOCUMENT SAFETY_DATA_SHEET SAFETY_SUMMARY_SHEET SAMPLE_SHIPPING_ORDER SUMMARY_OF_PRODUCT_CHARACTERISTICS TESTING_METHODOLOGY_RESULTS TRADE_ITEM_DESCRIPTION TRADE_ITEM_IMAGE_WITH_DIMENSIONS VIDEO WARRANTY_INFORMATION WEBSITE ZOOM_VIEW', '\s') satisfies $code = normalize-space(.)"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                                test="some $code in tokenize('360_DEGREE_IMAGE ASSEMBLY_INSTRUCTIONS AUDIO CERTIFICATION CHEMICAL_ASSESSMENT_SUMMARY CHEMICAL_SAFETY_REPORT CHILD_NUTRITION_LABEL CONSUMER_HANDLING_AND_STORAGE CROSSSECTION_VIEW DIET_CERTIFICATE DOCUMENT DOP_SHEET DRUG_FACT_LABEL ENERGY_LABEL GROUP_CHARACTERISTIC_SHEET HAZARDOUS_SUBSTANCES_DATA IFU INTERNAL_VIEW LOGO MARKETING_INFORMATION MATERIAL_SAMPLES MOBILE_DEVICE_IMAGE NUTRITION_FACT_LABEL ORGANIC_CERTIFICATE OTHER_EXTERNAL_INFORMATION OUT_OF_PACKAGE_IMAGE PACKAGING_ARTWORK PLANOGRAM PRODUCT_FORMULATION_STATEMENT PRODUCT_IMAGE PRODUCT_LABEL_IMAGE PRODUCT_WEBSITE QR_CODE QUALITY_CONTROL_PLAN RECIPE_WEBSITE REGULATORY_INSPECTION_AUDIT RISK_ANALYSIS_DOCUMENT SAFETY_DATA_SHEET SAFETY_SUMMARY_SHEET SAMPLE_SHIPPING_ORDER SUMMARY_OF_PRODUCT_CHARACTERISTICS TESTING_METHODOLOGY_RESULTS TRADE_ITEM_DESCRIPTION TRADE_ITEM_IMAGE_WITH_DIMENSIONS VIDEO WARRANTY_INFORMATION WEBSITE ZOOM_VIEW', '\s') satisfies $code = normalize-space(.)">
               <xsl:attribute name="id">NOGOV-T19-R020</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R020]-Use of ReferencedFileTypeCode version 5 code list requires to use the codes defined in the code list.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cac:AdditionalItemProperty[normalize-space(cbc:Name) = 'STERILE']"
                 priority="1001"
                 mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                       context="cac:AdditionalItemProperty[normalize-space(cbc:Name) = 'STERILE']"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="normalize-space(cbc:Value) = 'NO' or cbc:ValueQualifier"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                                test="normalize-space(cbc:Value) = 'NO' or cbc:ValueQualifier">
               <xsl:attribute name="id">NOGOV-T19-R021</xsl:attribute>
               <xsl:attribute name="flag">warning</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R021]-Use of ValueQualifier is recommended for values except 'NO'.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="some $code in tokenize('gs1:SterilisationTypeCode', '\s') satisfies $code = normalize-space(cbc:ValueQualifier)"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                                test="some $code in tokenize('gs1:SterilisationTypeCode', '\s') satisfies $code = normalize-space(cbc:ValueQualifier)">
               <xsl:attribute name="id">NOGOV-T19-R022</xsl:attribute>
               <xsl:attribute name="flag">warning</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R022]-Non-recommended code list is specified as qualifier.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>

	  <!--RULE -->
   <xsl:template match="cac:AdditionalItemProperty[normalize-space(cbc:ValueQualifier) = 'gs1:SterilisationTypeCode']/cbc:Value"
                 priority="1000"
                 mode="M4">
      <svrl:fired-rule xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                       context="cac:AdditionalItemProperty[normalize-space(cbc:ValueQualifier) = 'gs1:SterilisationTypeCode']/cbc:Value"/>

		    <!--ASSERT -->
      <xsl:choose>
         <xsl:when test="some $code in tokenize('AUTOCLAVE BETA_RADIATION CHLORINE_DIOXIDE DRY_HEAT ELECTRON_BEAM_IRRADIATION ETHANOL ETO_ETHYLENE_OXIDE FORMALDEHYDE GAMMA_RADIATION GLUTARALDEHYDE HIGH_INTENSITY_OR_PULSE_LIGHT HIGH_LEVEL_DISINFECTANT HYDROGEN_PEROXIDE LIQUID_CHEMICAL MICROWAVE_RADIATION NITROGEN_DIOXIDE OZONE PERACETIC_ACID PLASMA SOUND_WAVES SUPERCRITICAL_CARBON_DIOXIDE UNSPECIFIED UV_LIGHT', '\s') satisfies $code = normalize-space(.)"/>
         <xsl:otherwise>
            <svrl:failed-assert xmlns:svrl="http://purl.oclc.org/dsdl/svrl"
                                test="some $code in tokenize('AUTOCLAVE BETA_RADIATION CHLORINE_DIOXIDE DRY_HEAT ELECTRON_BEAM_IRRADIATION ETHANOL ETO_ETHYLENE_OXIDE FORMALDEHYDE GAMMA_RADIATION GLUTARALDEHYDE HIGH_INTENSITY_OR_PULSE_LIGHT HIGH_LEVEL_DISINFECTANT HYDROGEN_PEROXIDE LIQUID_CHEMICAL MICROWAVE_RADIATION NITROGEN_DIOXIDE OZONE PERACETIC_ACID PLASMA SOUND_WAVES SUPERCRITICAL_CARBON_DIOXIDE UNSPECIFIED UV_LIGHT', '\s') satisfies $code = normalize-space(.)">
               <xsl:attribute name="id">NOGOV-T19-R023</xsl:attribute>
               <xsl:attribute name="flag">fatal</xsl:attribute>
               <xsl:attribute name="location">
                  <xsl:apply-templates select="." mode="schematron-select-full-path"/>
               </xsl:attribute>
               <svrl:text>[NOGOV-T19-R023]-Use of SterilisationTypeCode version 2 code list requires to use the codes defined in the code list.</svrl:text>
            </svrl:failed-assert>
         </xsl:otherwise>
      </xsl:choose>
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>
   <xsl:template match="text()" priority="-1" mode="M4"/>
   <xsl:template match="@*|node()" priority="-2" mode="M4">
      <xsl:apply-templates select="@*|*" mode="M4"/>
   </xsl:template>
</xsl:stylesheet>
