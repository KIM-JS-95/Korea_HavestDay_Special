package org.air.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.air.config.AWStextrack;

import org.air.config.CustomCode;

import org.air.entity.*;
import org.air.repository.NationCodeRepository;
import org.air.repository.ScheduleRepository;
import org.air.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.textract.TextractClient;
import software.amazon.awssdk.services.textract.model.Block;

import javax.servlet.ServletContext;
import javax.transaction.Transactional;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

import java.text.SimpleDateFormat;
import java.util.*;

import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.air.config.AWStextrack.saveBlocksToJson;

@Service
@Configuration
public class ScheduleService {
    @Autowired
    private AWStextrack awstextrack;
    @Autowired
    private ScheduleRepository scheduleRepository;
    @Autowired
    private NationCodeRepository nationCodeRepository;
    @Autowired
    private UserRepository userRepository;

    @Autowired
    private CustomUserDetailService customUserDetailService;

    @Autowired
    private FcmServiceImpl fcmService;

    @Autowired
    private ServletContext servletContext;


    //@Transactional // clear
    public List<FlightData> getTodaySchedules(String userid, String startDate) {
        User user = userRepository.findByUserid(userid);
        String auth = user.getAuthority().getAuthority();

        List<Schedule> schedules = new ArrayList<>();
        if (auth.equals("USER")) {
            schedules = scheduleRepository.findByUserAndDate(user, startDate);
        } else if (auth.equals("FAMILY")) {
            User family = userRepository.findByUserid(user.getFamily());
            schedules = scheduleRepository.findByUserAndDate(family, startDate);
        } else {
        } // auth.equals("ADMIN")

        AtomicReference<String> previousDateRef = new AtomicReference<>();
        Stream<Schedule> updatedStream = schedules.stream().map(s -> {
            String date = s.getDate();
            if (s.getDate() == null) {
                s.setDate(previousDateRef.get()); // date가 비어 있으면 이전 값을 사용
            } else {
                previousDateRef.set(date);
            }
            return s;
        });
        List<Schedule> updatedSchedules = updatedStream.collect(Collectors.toList());
        Map<String, Map<String, String>> code = getNationCode();

        return generateFlightData(updatedSchedules, code);
    }

    // clear
    public List<FlightData> getViewSchedule(Long id) {
        Schedule schedule = scheduleRepository.findById(id).orElseThrow();

        // 크기가 고정된 리스트
        List<Schedule> schedules = Collections.singletonList(schedule);
        Map<String, Map<String, String>> code = getNationCode();
        return generateFlightData(schedules, code);
    }

    // clear
    public List<FlightData> getAllSchedules(String userid) {
        User user = userRepository.findByUserid(userid);
        String auth = user.getAuthority().getAuthority();

        List<Schedule> schedules = new ArrayList<>();
        if (auth.equals("USER")) {
            schedules = scheduleRepository.findByUserUserid(user.getUserid());
        } else if (auth.equals("FAMILY")) {
            schedules = scheduleRepository.findByUserUserid(user.getFamily());
        }

        AtomicReference<String> previousDateRef = new AtomicReference<>();
        Stream<Schedule> updatedStream = schedules.stream().map(s -> {
            String date = s.getDate();
            if (s.getDate() == null) {
                s.setDate(previousDateRef.get()); // date가 비어 있으면 이전 값을 사용
            } else {
                previousDateRef.set(date);
            }
            return s;
        });

        List<Schedule> updatedSchedules = updatedStream.collect(Collectors.toList());
        Map<String, Map<String, String>> code = getNationCode();

        return generateFlightData(updatedSchedules, code);
    }


    // -------------------------- 일정 수정 삭제 저장 --------------------------
    @Transactional
    public List<Schedule> schedule_save(List<Schedule> schedules, String userid) {
        User user = userRepository.findByUserid(userid);
        try {
            for (int i = 0; i < schedules.size(); i++) {
                if (schedules.get(i).getDate() == null && i > 0) {
                    schedules.get(i).setDate(schedules.get(i - 1).getDate());
                }
                if (schedules.get(i).getCi() == null && i > 0) {
                    schedules.get(i).setCi(schedules.get(i - 1).getCi());
                }
                schedules.get(i).setUser(user); // 일정의 주인 추가
            }
            List<Schedule> result = scheduleRepository.saveAll(schedules);
            fcmService.completeSave_Schedule(userid); // 알림
            return result;
        } catch (RuntimeException e) {
            e.printStackTrace(); // 예외 로그 출력
            return new ArrayList<>();
        }
    }

    @Transactional
    public CustomCode modify(Schedule update_schedule) {
        Optional<Schedule> optionalSchedule = scheduleRepository.findById(update_schedule.getId());

        if (optionalSchedule.isPresent()) {
            Schedule schedule = optionalSchedule.get();
            try {
                schedule.setDate(update_schedule.getDate());
                schedule.setPairing(update_schedule.getPairing());
                schedule.setDc(update_schedule.getDc());
                schedule.setCi(update_schedule.getCi());
                schedule.setCo(update_schedule.getCo());
                schedule.setActivity(update_schedule.getActivity());

                schedule.setCntFrom(update_schedule.getCntFrom());

                schedule.setStdL(update_schedule.getStdL());
                schedule.setStdB(update_schedule.getStdB());

                schedule.setCntTo(update_schedule.getCntTo());

                schedule.setStaL(update_schedule.getStaL());
                schedule.setStaB(update_schedule.getStaB());

                schedule.setAchotel(update_schedule.getAchotel());
                schedule.setBlk(update_schedule.getBlk());

                fcmService.sendMessageTo(update_schedule.getDate(), update_schedule.getCntFrom(), update_schedule.getCntTo(), schedule.getUser());

                return new CustomCode(StatusEnum.OK);
            } catch (Exception e) {
                System.out.println(e.getMessage());
                return new CustomCode(StatusEnum.BAD_REQUEST);
            }
        } else {
            return new CustomCode(StatusEnum.NOT_FOUND); // 혹은 다른 적절한 예외를 던지도록 처리
        }
    }

    // 파일럿 일정 전체 삭제
    public boolean delete(String userid) {
        try {
            scheduleRepository.deleteAllByUserUserid(userid);
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    public boolean delete_cron(String month){
        return scheduleRepository.delete_cron(month);
    }

    public List<Schedule> textrack(String userid, InputStream source) throws IOException {

        HashMap<String, String> map = new HashMap<>();
        List<Block> list_block = new ArrayList<>();

        TextractClient textractClient = awstextrack.awsceesser();
        List<Block> blocks = AWStextrack.analyzeDoc(textractClient, source);
        saveBlocksToJson(blocks, "src/main/resources/textreck_schedule" + userid + ".json");


        Date today = new Date();
        SimpleDateFormat dateFormat = new SimpleDateFormat("MM");
        customUserDetailService.set_schedule_status(userid, Integer.parseInt(dateFormat.format(today))); // 일정 상태 저장

        blocks.forEach(callback -> {
            if (Objects.equals(callback.blockType().toString(), "WORD")) {
                map.put(callback.id(), callback.text());
            } else if (Objects.equals(callback.blockType().toString(), "CELL")) {
                list_block.add(callback);
            }
        });

        return AWStextrack.texttoEntity(map, list_block);
    }

    // -------------------------- Nation_Code Mapping --------------------------
    public List<FlightData> generateFlightData(List<Schedule> sList, Map<String, Map<String, String>> codes) {
        List<FlightData> flightDataList = new ArrayList<>();

        for (Schedule s : sList) {
            FlightData flightData = FlightData.builder()
                    .departureShort(s.getCntFrom())
                    .departure(codes.getOrDefault(s.getCntFrom(), Collections.emptyMap()).getOrDefault("code", "Unknown"))
                    .date(s.getDate())
                    .destinationShort(s.getCntTo())
                    .destination(codes.getOrDefault(s.getCntTo(), Collections.emptyMap()).getOrDefault("code", "Unknown"))
                    .flightNumber(s.getPairing())
                    .stal(s.getStaL())
                    .stab(s.getStaB())
                    .stdl(s.getStdL())
                    .stdb(s.getStdB())
                    .activity(s.getActivity())
                    .id(s.getId())
                    .ci(s.getCi())
                    .co(s.getCo())
                    .lat(codes.getOrDefault(s.getCntTo(), Collections.emptyMap()).getOrDefault("lat", "unknown"))
                    .lon(codes.getOrDefault(s.getCntTo(), Collections.emptyMap()).getOrDefault("lon", "unknown"))
                    .build();
            flightDataList.add(flightData);
        }

        return flightDataList;
    }


    public Map<String, Map<String, String>> getNationCode() {
        List<NationCode> codes = nationCodeRepository.findAll();
        Map<String, Map<String, String>> codeCountryMap = convertToMap(codes);
        return codeCountryMap;
    }

    public Map<String, Map<String, String>> convertToMap(List<NationCode> nationCodes) {
        Map<String, Map<String, String>> codeCountryMap = new HashMap<>();

        for (NationCode nationCode : nationCodes) {
            Map<String, String> metadate = new HashMap<>();
            metadate.put("code", nationCode.getCode()); // code
            metadate.put("lat", nationCode.getLat()); // lat
            metadate.put("lon", nationCode.getLon()); // lon
            codeCountryMap.put(nationCode.getCountry(), metadate);
        }
        return codeCountryMap;
    }

    public List<Block> getScheduleJsonData(String userid, ServletContext context) {

        List<Block> blocks = new ArrayList<>();
        try {
            String dir = "/static/img/textreck_schedule/" + userid + ".json";
            String absoluteDir = context.getRealPath(dir);
            File jsonFile = new File(absoluteDir);

            if (jsonFile.exists()) {
                ObjectMapper objectMapper = new ObjectMapper();
                blocks = objectMapper.readValue(jsonFile, new TypeReference<List<Block>>() {
                });
            } else {
                System.out.println("JSON file not found.");
            }

        } catch (IOException e) {
            e.printStackTrace();
        }

        return blocks;
    }
}
