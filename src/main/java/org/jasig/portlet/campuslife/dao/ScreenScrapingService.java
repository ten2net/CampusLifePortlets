/**
 * Licensed to Jasig under one or more contributor license
 * agreements. See the NOTICE file distributed with this work
 * for additional information regarding copyright ownership.
 * Jasig licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a
 * copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on
 * an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package org.jasig.portlet.campuslife.dao;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerConfigurationException;
import javax.xml.transform.TransformerException;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.stream.StreamResult;
import javax.xml.transform.stream.StreamSource;

import org.apache.commons.io.IOUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.owasp.validator.html.AntiSamy;
import org.owasp.validator.html.CleanResults;
import org.owasp.validator.html.Policy;
import org.owasp.validator.html.PolicyException;
import org.owasp.validator.html.ScanException;
import org.springframework.core.io.Resource;

public abstract class ScreenScrapingService<T> {
    protected final Log log = LogFactory.getLog(getClass());

    private static final TransformerFactory transformerFactory = TransformerFactory.newInstance();

    private Resource xslt;
    
    /**
     * Set the XSLT to use when transforming clean, valid HTML to portlet-specific
     * XML.
     * 
     * @param xslt
     */
    public void setXslt(Resource xslt) {
        this.xslt = xslt;
    }
    
    private Policy policy;
    
    /**
     * Set the AntiSamy policy to be used to clean and validate HTML content.
     * 
     * @param config
     * @throws PolicyException
     * @throws IOException
     */
    public void setPolicy(Resource config) throws PolicyException, IOException {
        this.policy = Policy.getInstance(config.getFile());
    }
    
    public abstract String getPackageName();
    
    private List<IScreenScrapingPostProcessor<T>> postProcessors;
    
    /**
     * 
     * @param postProcessors
     */
    public void setPostProcessors(List<IScreenScrapingPostProcessor<T>> postProcessors) {
        this.postProcessors = postProcessors;
    }
    
    public T getItem(String key, String url) {
        try {
            
            log.debug(url);
            final String htmlContent = getHtmlContent(url);
            final String cleanedContent = getCleanedHtmlContent(htmlContent);
            final String xml = getXml(cleanedContent);
            log.debug(xml);
            final T item = deserializeItem(xml);
            
            for (IScreenScrapingPostProcessor<T> processor : postProcessors) {
                processor.postProcess(key, item);
            }
            
            return item;
            
        } catch (ClientProtocolException e) {
            log.error("Failed to retrieve dining hall menu", e);
        } catch (TransformerConfigurationException e) {
            throw new RuntimeException("Failed to create identity transformer to serialize Node to String", e);
        } catch (TransformerException e) {
            throw new RuntimeException("Failed to convert Node to String using Transformer", e);
        } catch (IOException e) {
            log.error("Failed to retrieve dining hall menu", e);
        } catch (ScanException e) {
            log.error("Failed to parse dining hall menu", e);
        } catch (PolicyException e) {
            log.error("Failed to parse dining hall menu", e);
        } catch (JAXBException e) {
            log.error("Failed to parse dining hall menu", e);
        }
        
        return null;
    }
    
    /**
     * Get HTML content for the specified dining hall.
     * 
     * @param diningHall
     * @return
     * @throws ClientProtocolException
     * @throws IOException
     */
    protected String getHtmlContent(String url) throws ClientProtocolException, IOException {
        final DefaultHttpClient httpclient = new DefaultHttpClient();
        final HttpGet httpget = new HttpGet(url);
        final HttpResponse response = httpclient.execute(httpget);
        final InputStream httpStream = response.getEntity().getContent();
        final String content = IOUtils.toString(httpStream);
        return content;
    }
    
    /**
     * Get clean, valid HTML for the supplied HTML markup.
     * 
     * @param html
     * @return
     * @throws ScanException
     * @throws PolicyException
     */
    protected String getCleanedHtmlContent(String html) throws ScanException, PolicyException {
        final AntiSamy as = new AntiSamy();
        final CleanResults cr = as.scan(html, policy);
        final String cleanedHtml = cr.getCleanHTML();  
        final String unescaped = cleanedHtml.replace("&", "&amp;");
        return unescaped;
    }

    /**
     * Get portlet-specific XML for clean, valid HTML.
     * 
     * @param cleanHtml
     * @return
     * @throws TransformerException
     * @throws IOException
     */
    protected String getXml(String cleanHtml) throws TransformerException, IOException {
        final StreamSource xsltSource = new StreamSource(xslt.getInputStream());
        final Transformer identityTransformer = transformerFactory.newTransformer(xsltSource);
        identityTransformer.setOutputProperty(OutputKeys.OMIT_XML_DECLARATION, "yes");
        final StringWriter outputWriter = new StringWriter();
        final StreamResult outputTarget = new StreamResult(outputWriter);
        final StreamSource xmlSource = new StreamSource(new StringReader(cleanHtml));
        identityTransformer.transform(xmlSource, outputTarget);
        final String content = outputWriter.toString();
        return content;
    }
    
    /**
     * Deserialize a menu from the provided XML.
     * 
     * @param xml
     * @return
     * @throws JAXBException
     */
    protected T deserializeItem(String xml) throws JAXBException {
        
        final JAXBContext jc = JAXBContext.newInstance( getPackageName() );
        final Unmarshaller u = jc.createUnmarshaller();
        
        @SuppressWarnings("unchecked")
        final T menu = (T) u.unmarshal(IOUtils.toInputStream(xml));
        return menu;
    }


}
