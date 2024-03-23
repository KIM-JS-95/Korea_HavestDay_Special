
package org.air.config;

import org.air.entity.Schedule;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Pattern;

@Configuration
public class AWStextrack {

    @Value("${accesskey}")
    private String accesskey;

    @Value("${secretkey}")
    private String secretkey;

    public TextractClient awsceesser() {
        AwsBasicCredentials awsCreds = AwsBasicCredentials.create(accesskey, secretkey);
        TextractClient textractClient = TextractClient.builder()
                .region(Region.US_WEST_2)
                .credentialsProvider(StaticCredentialsProvider.create(awsCreds))
                .build();
        return textractClient;
    }


    public static List<Block> analyzeDoc(TextractClient textractClient, InputStream sourceDoc) {
        List<Block> docInfo = null;
        try {
            SdkBytes sourceBytes = SdkBytes.fromInputStream(sourceDoc);
            Document myDoc = Document.builder()
                    .bytes(sourceBytes)
                    .build();

            List<FeatureType> featureTypes = new ArrayList<FeatureType>();
            featureTypes.add(FeatureType.TABLES);
            featureTypes.add(FeatureType.FORMS);

            AnalyzeDocumentRequest analyzeDocumentRequest = AnalyzeDocumentRequest
                    .builder()
                    .featureTypes(featureTypes)
                    .document(myDoc)
                    .build();

            AnalyzeDocumentResponse analyzeDocument = textractClient
                    .analyzeDocument(analyzeDocumentRequest);

            docInfo = analyzeDocument.blocks();

        } catch (TextractException e) {
            System.err.println(e.getMessage());
            System.exit(1);
        }
        return docInfo;
    }

    public static boolean isDateValid(String dateString) {
        try {
            String dateFormatPattern = "^\\d{2}(Jan|Feb|Mar|Apr|May|Jun|Jul|Aug|Sep|Oct|Nov|Dec)\\d{2}$";
            return Pattern.matches(dateFormatPattern, dateString);
        } catch (Exception e) {
            return false;
        }
    }

    @Bean
    public static List<Schedule> texttoEntity(HashMap<String, String> map, List<Block> list) {
        List<Schedule> schedules = new ArrayList<>();
        Schedule schedule = new Schedule();
        String[] weeks = {"null"};
        try {
            for (int i = 0; i < list.size(); i++) {
                Block block = list.get(i);
                int index = block.columnIndex();
                System.out.println(block.toString());
                String[] ids = block.relationships().isEmpty() ? weeks : block.relationships().get(0).ids().toArray(new String[0]);
                if (list.get(i).rowIndex() == 1) continue;
                if (index == 13) {
                    schedules.add(schedule);
                    schedule = new Schedule();
                }
                if (block.relationships() != null) {
                    if (index == 1) {
                        if (ids.length == 1) {
                            if (isDateValid(map.get(ids[0]))) {
                                schedule.setDate(map.get(ids[0]));
                            } else {
                                schedule.setPairing(map.get(ids[0]));
                            }
                        } else {
                            schedule.setDate(map.get(ids[0]));
                            schedule.setPairing(map.get(ids[1]));
                        }
                    } else if (index == 2) {
                        schedule.setPairing(map.get(ids[0]));
                    } else if (index == 3) {
                        schedule.setDc(map.get(ids[0]));
                    } else if (index == 4) {
                        schedule.setCi(map.get(ids[0]));
                    } else if (index == 5) {
                        schedule.setCo(map.get(ids[0]));
                    } else if (index == 6) {
                        schedule.setActivity(map.get(ids[0]));
                    } else if (index == 7) {

                        schedule.setCntFrom(map.get(ids[0]));
                    } else if (index == 8) {
                        schedule.setStdL(map.get(ids[0]));
                    } else if (index == 9) {
                        schedule.setStdB(map.get(ids[0]));
                    } else if (index == 10) {

                        schedule.setCntTo(map.get(ids[0]));
                    } else if (index == 11) {
                        schedule.setStaL(map.get(ids[0]));
                    } else if (index == 12) {
                        schedule.setStaB(map.get(ids[0]));
                    } else if (index == 13) {
                        String hotel = "";
                        for (int j = 0; j < ids.length; j++) {
                            hotel += map.get(ids[j]);
                        }
                        schedule.setAchotel(hotel);
                    }
                }
            }
        } catch (Exception e) {
            throw e;
        }

        return schedules;
    }


}