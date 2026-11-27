package com.xywy.signalmap.data.importer

import org.xml.sax.EntityResolver
import org.xml.sax.SAXException
import org.xml.sax.SAXNotRecognizedException
import org.xml.sax.SAXNotSupportedException
import org.xml.sax.XMLReader
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.parsers.SAXParserFactory

internal object ImportXmlSecurity {
    private val features = arrayOf(
        "http://apache.org/xml/features/disallow-doctype-decl" to true,
        "http://xml.org/sax/features/external-general-entities" to false,
        "http://xml.org/sax/features/external-parameter-entities" to false,
        "http://apache.org/xml/features/nonvalidating/load-external-dtd" to false,
    )
    private val rejectExternalEntities = EntityResolver { _, _ ->
        throw SAXException("导入文件不允许引用外部 XML 内容")
    }

    fun newDocumentBuilder(): DocumentBuilder = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        applyFeatures(*features, setter = ::setFeature)
        applyOptionalActions(
            { isXIncludeAware = false },
            { isExpandEntityReferences = false },
        )
    }.newDocumentBuilder().apply { setEntityResolver(rejectExternalEntities) }

    fun newXmlReader(): XMLReader = SAXParserFactory.newInstance().apply {
        isNamespaceAware = true
        applyFeatures(*features, setter = ::setFeature)
    }.newSAXParser().xmlReader.apply { entityResolver = rejectExternalEntities }

    fun applyFeatures(vararg features: Pair<String, Boolean>, setter: (String, Boolean) -> Unit) {
        features.forEach { (feature, enabled) ->
            try {
                setter(feature, enabled)
            } catch (_: ParserConfigurationException) {
                // Android implementations differ; the entity resolver also blocks external reads.
            } catch (_: SAXNotRecognizedException) {
                // Unsupported SAX feature.
            } catch (_: SAXNotSupportedException) {
                // Unsupported SAX feature.
            }
        }
    }

    fun applyOptionalActions(vararg actions: () -> Unit) {
        actions.forEach { action ->
            try {
                action()
            } catch (_: UnsupportedOperationException) {
                // Optional on Android XML implementations.
            } catch (_: AbstractMethodError) {
                // Optional on older implementations.
            }
        }
    }
}
