package com.gfg.jbdl12urlshortener.manager;

import com.gfg.jbdl12urlshortener.entity.ClientConfiguration;
import com.gfg.jbdl12urlshortener.entity.LongUrl;
import com.gfg.jbdl12urlshortener.model.UrlCreationRequest;
import com.gfg.jbdl12urlshortener.repository.ClientRepository;
import com.gfg.jbdl12urlshortener.repository.LongUrlRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Base64;

@Service
public class UrlShortenerManagerImpl implements UrlShortenerManager {

    @Autowired
    private ClientRepository clientRepository;

    @Autowired
    private LongUrlRepository longUrlRepository;

    @Override
    public String createLongUrl(UrlCreationRequest urlCreationRequest) {
        LongUrl longUrl = LongUrl.builder()
                .url(urlCreationRequest.getLongUrl())
                        .createdAt(LocalDateTime.now())
                        .expired(false)
                        .build();

        ClientConfiguration clientConfiguration =clientRepository.findByHostName(urlCreationRequest.getHostName()).orElse(null);

        if(clientConfiguration==null){
            clientConfiguration = clientConfiguration.builder()
                    .hostName(urlCreationRequest.getHostName())
                    .longUrls(new ArrayList<>())
                    .build();
        }
        //check if the long url exists
        for(LongUrl url : clientConfiguration.getLongUrls()){
            if(url.getUrl().equals(urlCreationRequest.getLongUrl())){
                return buildShortUrl(clientConfiguration.getHostName(),url.getId());
            }
        }
        longUrl.setClientConfiguration(clientConfiguration);//long Url is also having reference of client configuration
        clientConfiguration.getLongUrls().add(longUrl);
        ClientConfiguration savedClientConfiguration=clientRepository.save(clientConfiguration);
        //If we store client configuration the child(LongURl) of client configuration will also be stored
        Long id=null;
            for(LongUrl url : savedClientConfiguration.getLongUrls()){
                if(url.getUrl().equals(urlCreationRequest.getLongUrl())){
                    id=url.getId();
                }
            }
        return buildShortUrl(clientConfiguration.getHostName(),id);
    }
    private String buildShortUrl(String hostname,Long longUrlId){
        return "http://".concat(hostname).concat("/").concat(encryptId(longUrlId));
    }

    String encryptId(Long id){
        String encryptedString =
                Base64.getEncoder().encodeToString(String.valueOf(id).getBytes());
        return encryptedString;
    }
    @Override
    public String getLongUrl(String encryptedId) throws Exception {
        Long id = decryptId(encryptedId);
        LongUrl longUrl = longUrlRepository.findById(id)
                .orElseThrow(()-> new Exception("url is not present"));
        return longUrl.getUrl();
    }

    Long decryptId(String encryptedId) throws UnsupportedEncodingException {
        String encryptedString =
                new String(Base64.getDecoder().decode(encryptedId),//byte array,
                        "UTF-8");//format of String
        return Long.parseLong(encryptedString);
    }
}
