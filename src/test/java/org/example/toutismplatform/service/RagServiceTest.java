package org.example.toutismplatform.service;

import dev.langchain4j.model.chat.ChatLanguageModel;
import org.example.toutismplatform.entity.LargeScenicArea;
import org.example.toutismplatform.entity.Product;
import org.example.toutismplatform.entity.SmallScenicSpot;
import org.example.toutismplatform.repository.LargeScenicAreaRepository;
import org.example.toutismplatform.repository.ProductRepository;
import org.example.toutismplatform.repository.SmallScenicSpotRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RagServiceTest {

    @Test
    void returnsUnrecordedMessageForUnknownSpecificScenicName() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "chatModel", model);

        when(largeRepository.findAll()).thenReturn(List.of(area("清明上河园")));
        when(smallRepository.findAll()).thenReturn(List.of());

        String answer = service.generateAnswer("开封的白马楼如何游玩");

        assertThat(answer).isEqualTo("开封目前没有这个景点，或者当前系统还没有录入这个景点。");
    }

    @Test
    void detectsUnknownNameEvenWhenQueryStartsWithRecommendation() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(area("清明上河园")));
        when(smallRepository.findAll()).thenReturn(List.of());

        String answer = service.generateAnswer("推荐白马楼怎么玩");

        assertThat(answer).isEqualTo("开封目前没有这个景点，或者当前系统还没有录入这个景点。");
    }

    @Test
    void doesNotSubstituteKnownScenicAreaForUnknownMultilineQuery() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(area("开封博物馆"), area("清明上河园")));
        when(smallRepository.findAll()).thenReturn(List.of());

        String answer = service.generateAnswer("你\n开封的白马楼如何游玩\n游");

        assertThat(answer).isEqualTo("开封目前没有这个景点，或者当前系统还没有录入这个景点。");
        assertThat(answer).doesNotContain("开封博物馆");
    }

    @Test
    void asksForClearTourismQuestionForWhitelistCheckOnlyInput() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(area("龙亭景区"), area("开封博物馆")));
        when(smallRepository.findAll()).thenReturn(List.of(spot("大雄宝殿"), spot("千手千眼观音殿")));

        String answer = service.generateAnswer("（5）白名单校验\n游");

        assertThat(answer).isEqualTo("请告诉我你想了解的开封景点、路线或游玩偏好，我可以帮你介绍景点或规划路线。");
        assertThat(answer).doesNotContain("龙亭景区", "开封博物馆", "大雄宝殿", "千手千眼观音殿");
    }

    @Test
    void recommendsFoodPlacesForGoodFoodQuery() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(
                foodArea(8L, "鼓楼夜市", "开封热门餐饮与夜间休闲聚集点。", "夜市,美食,休闲,晚间", 0),
                foodArea(15L, "开封第一楼（寺后街店）", "开封代表性的灌汤包老字号之一。", "老字号,灌汤包,豫菜,开封美食", 68),
                area(1L, "清明上河园", 0)
        ));
        when(smallRepository.findAll()).thenReturn(List.of());

        String answer = service.generateAnswer("有哪些好吃的地方\n游");

        assertThat(answer)
                .contains("鼓楼夜市")
                .contains("开封第一楼（寺后街店）")
                .doesNotContain("请告诉我你想了解的开封景点");
    }

    @Test
    void recommendsPackagesForDiscountPackageQuery() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "productRepository", productRepository);

        when(largeRepository.findAll()).thenReturn(List.of(area("清明上河园")));
        when(smallRepository.findAll()).thenReturn(List.of());
        when(productRepository.findOnSaleWithScenicAreas("ON_SALE")).thenReturn(List.of(
                product(2L, "开封经典初游一日套餐", "推荐路线：开封站到清明上河园。", 159),
                product(5L, "古迹园林休闲漫游套餐", "适合喜欢古塔、园林和轻松慢游的游客。", 89)
        ));

        String answer = service.generateAnswer("有哪些优惠套餐\n游");

        assertThat(answer)
                .contains("开封经典初游一日套餐")
                .contains("古迹园林休闲漫游套餐")
                .doesNotContain("请告诉我你想了解的开封景点");
    }

    @Test
    void usesModelIntentClassifierForIndirectPackageRequest() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ProductRepository productRepository = mock(ProductRepository.class);
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "productRepository", productRepository);
        ReflectionTestUtils.setField(service, "chatModel", model);

        when(largeRepository.findAll()).thenReturn(List.of(area("清明上河园")));
        when(smallRepository.findAll()).thenReturn(List.of());
        when(productRepository.findOnSaleWithScenicAreas("ON_SALE")).thenReturn(List.of(
                product(2L, "开封经典初游一日套餐", "推荐路线：开封站到清明上河园。", 159)
        ));
        when(model.generate(anyString())).thenReturn(
                "intent=PACKAGE_RECOMMENDATION\nscenicName=\nmaxStops=0\nreason=省钱组合",
                "开封经典初游一日套餐包含已确认的路线信息，价格159.00元。"
        );

        String answer = service.generateAnswer("帮我挑一份省钱组合");

        verify(model, org.mockito.Mockito.atLeast(2)).generate(anyString());
        assertThat(answer)
                .contains("开封经典初游一日套餐")
                .contains("159.00元")
                .doesNotContain("请告诉我你想了解的开封景点");
    }

    @Test
    void recommendsGeneralScenicPlacesForFunPlaceQuery() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "chatModel", model);

        when(largeRepository.findAll()).thenReturn(List.of(
                area(1L, "清明上河园", 0),
                area(3L, "龙亭景区", 0),
                area(4L, "开封府", 0)
        ));
        when(smallRepository.findAll()).thenReturn(List.of());
        when(model.generate(anyString())).thenReturn(
                "intent=GENERAL_SCENIC_LIST\nscenicName=\nmaxStops=0\nreason=推荐景点",
                "清明上河园、龙亭景区和开封府都是开封真实可推荐的景区。"
        );

        String answer = service.generateAnswer("有哪些好玩的地方\n游");

        verify(model, org.mockito.Mockito.atLeast(2)).generate(anyString());
        assertThat(answer)
                .contains("清明上河园")
                .contains("龙亭景区")
                .contains("开封府")
                .contains("购物车")
                .doesNotContain("请告诉我你想了解的开封景点");
    }

    @Test
    void treatsFamilyFriendlyScenicQueryAsGeneralRecommendation() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(
                taggedArea(1L, "清明上河园", "亲子,儿童,家庭,演艺", 0),
                taggedArea(3L, "禹王台公园", "历史公园,园林,散步,休闲", 0)
        ));
        when(smallRepository.findAll()).thenReturn(List.of());

        String answer = service.generateAnswer("有哪些适合亲子游玩的景区\n游");

        assertThat(answer)
                .contains("开封市有很多适合亲子游玩的地方，以下是一些推荐")
                .contains("清明上河园")
                .contains("作为以亲子游玩")
                .doesNotContain("开封目前没有这个景点")
                .doesNotContain("景区资料")
                .doesNotContain("特色：");
    }

    @Test
    void doesNotExposeInternalFactHeadingWhenModelUnavailableForElderlyQuery() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(
                taggedArea(5L, "大相国寺", "寺庙,人文,静态游览,老人友好", 0),
                taggedArea(6L, "铁塔景区", "古塔,园林,散步,拍照", 0)
        ));
        when(smallRepository.findAll()).thenReturn(List.of());

        String answer = service.generateAnswer("有哪些适合老人去的地方\n游");

        assertThat(answer)
                .contains("开封市有很多适合带老人去的地方，以下是一些推荐")
                .contains("大相国寺")
                .contains("作为以寺庙人文")
                .contains("更适合老年人慢节奏参观")
                .doesNotContain("景区资料")
                .doesNotContain("特色：");
    }

    @Test
    void prefersScoreSignalsOverTagCountForElderlyRecommendations() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        LargeScenicArea manyTagsLowScore = taggedArea(7L, "标签齐全低分园", "老人友好,静态游览,休闲,散步,园林,人文", 0);
        manyTagsLowScore.setElderlyFriendlyScore(BigDecimal.valueOf(1));
        manyTagsLowScore.setLeisureScore(BigDecimal.valueOf(1));
        manyTagsLowScore.setRestroomConvenienceScore(BigDecimal.valueOf(1));
        manyTagsLowScore.setIntensityLevel(5);
        manyTagsLowScore.setCrowdLevel(5);
        manyTagsLowScore.setPopularityScore(BigDecimal.valueOf(1));

        LargeScenicArea fewerTagsHighScore = taggedArea(8L, "高分静养园", "休闲", 0);
        fewerTagsHighScore.setElderlyFriendlyScore(BigDecimal.valueOf(5));
        fewerTagsHighScore.setLeisureScore(BigDecimal.valueOf(5));
        fewerTagsHighScore.setRestroomConvenienceScore(BigDecimal.valueOf(5));
        fewerTagsHighScore.setFoodConvenienceScore(BigDecimal.valueOf(4));
        fewerTagsHighScore.setIntensityLevel(1);
        fewerTagsHighScore.setCrowdLevel(1);
        fewerTagsHighScore.setPopularityScore(BigDecimal.valueOf(2));

        when(largeRepository.findAll()).thenReturn(List.of(manyTagsLowScore, fewerTagsHighScore));
        when(smallRepository.findAll()).thenReturn(List.of());

        String answer = service.generateAnswer("有哪些适合老人去的地方\n游");

        assertThat(answer)
                .contains("高分静养园")
                .contains("标签齐全低分园");
        assertThat(answer.indexOf("高分静养园")).isLessThan(answer.indexOf("标签齐全低分园"));
    }

    @Test
    void usesDatabaseInsideRouteForKnownScenicTourQuery() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(
                area(2L, "清明上河园", 0),
                area(4L, "开封府", 0),
                area(5L, "大相国寺", 0)
        ));
        when(smallRepository.findAll()).thenReturn(List.of(
                spot(1L, 2L, "虹桥"),
                spot(2L, 2L, "东京码头"),
                spot(3L, 2L, "拂云阁")
        ));

        String answer = service.generateAnswer("清明上河园如何游玩\n游");

        assertThat(answer).contains("景区：清明上河园")
                .contains("推荐园内顺序")
                .contains("虹桥")
                .contains("东京码头")
                .contains("拂云阁")
                .doesNotContain("开封府的大相国寺", "属于大相国寺", "景区内景观");
    }

    @Test
    void treatsShortPlanQueryAsRoutePlanningIntent() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        PathService pathService = mock(PathService.class);
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "pathService", pathService);
        ReflectionTestUtils.setField(service, "chatModel", model);

        when(largeRepository.findAll()).thenReturn(List.of(
                area(1L, "清明上河园", 0),
                area(2L, "开封站", 1),
                area(3L, "龙亭景区", 0),
                area(4L, "开封府", 0),
                area(5L, "大相国寺", 0)
        ));
        when(smallRepository.findAll()).thenReturn(List.of());
        when(pathService.recommendCityRoute(eq(null), eq(null), anyMap(), anyString(), eq(3)))
                .thenReturn(cityRouteResult());

        String answer = service.generateAnswer("方案\n游");

        verify(pathService).recommendCityRoute(eq(null), eq(null), anyMap(), anyString(), eq(3));
        assertThat(answer).contains("已为你整理出一条游玩路线")
                .doesNotContain("请告诉我你想了解的开封景点");
    }

    @Test
    void treatsScenicAreaAliasAsSingleAreaTourIntent() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(
                area(2L, "清明上河园", 0),
                area(6L, "铁塔景区", 0)
        ));
        when(smallRepository.findAll()).thenReturn(List.of(
                spot(10L, 6L, "铁塔主景点"),
                spot(11L, 6L, "塔影步道")
        ));

        String answer = service.generateAnswer("铁塔如何游玩\n游");

        assertThat(answer)
                .contains("景区：铁塔景区")
                .contains("推荐园内顺序")
                .contains("铁塔主景点")
                .contains("塔影步道")
                .doesNotContain("清明上河园");
    }

    @Test
    void treatsUnknownTowerNameAsUnrecordedScenicName() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        when(largeRepository.findAll()).thenReturn(List.of(area(6L, "铁塔景区", 0)));
        when(smallRepository.findAll()).thenReturn(List.of());

        String answer = service.generateAnswer("白马塔如何游玩\n游");

        assertThat(answer).isEqualTo("开封目前没有这个景点，或者当前系统还没有录入这个景点。");
    }

    @Test
    void treatsMoreScenicAreasAsRouteRequestInsteadOfUnknownScenicName() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        PathService pathService = mock(PathService.class);
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "pathService", pathService);
        ReflectionTestUtils.setField(service, "chatModel", model);

        List<LargeScenicArea> areas = List.of(
                area(1L, "清明上河园", 0),
                area(2L, "开封站", 1),
                area(3L, "龙亭景区", 0),
                area(4L, "开封府", 0),
                area(5L, "大相国寺", 0),
                area(6L, "铁塔公园", 0)
        );
        when(largeRepository.findAll()).thenReturn(areas);
        when(smallRepository.findAll()).thenReturn(List.of());
        when(model.generate(anyString())).thenReturn("intent=ROUTE_PLAN\nscenicName=\nmaxStops=5\nreason=更多方案");
        when(pathService.recommendCityRoute(eq(null), eq(null), anyMap(), anyString(), eq(5)))
                .thenReturn(cityRouteResult());

        String answer = service.generateAnswer("提供更多景区的方案\n游");

        verify(pathService).recommendCityRoute(eq(null), eq(null), anyMap(), anyString(), eq(5));
        verify(model).generate(anyString());
        assertThat(answer)
                .contains("本次共安排5个景区")
                .contains("清明上河园")
                .doesNotContain("开封目前没有这个景点");
    }

    @Test
    void treatsRailwayStationAsRouteEndpointInsteadOfUnknownScenicArea() {
        RagService service = new RagService();
        LinkedHashSet<String> allowedNames = new LinkedHashSet<>(List.of("清明上河园", "开封站"));
        String query = "从清明上河园开始，火车站结束，途径5个景区的方案";

        Boolean unrecorded = ReflectionTestUtils.invokeMethod(
                service,
                "isUnrecordedSpecificScenicQuery",
                query,
                allowedNames
        );
        Boolean pathPlanning = ReflectionTestUtils.invokeMethod(service, "isPathPlanningQuery", query);
        Map<String, String> locations = ReflectionTestUtils.invokeMethod(
                service,
                "extractLocations",
                query,
                List.of(area("清明上河园"), area("开封站"))
        );

        assertThat(unrecorded).isFalse();
        assertThat(pathPlanning).isTrue();
        assertThat(locations).containsEntry("start", "清明上河园")
                .containsEntry("end", "开封站");
    }

    @Test
    void usesCityRouteWhenStartEndAndFiveScenicStopsAreRequested() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        PathService pathService = mock(PathService.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "pathService", pathService);

        List<LargeScenicArea> areas = List.of(
                area(1L, "清明上河园", 0),
                area(2L, "开封站", 1),
                area(3L, "龙亭景区", 0),
                area(4L, "开封府", 0),
                area(5L, "大相国寺", 0),
                area(6L, "铁塔公园", 0)
        );
        when(largeRepository.findAll()).thenReturn(areas);
        when(smallRepository.findAll()).thenReturn(List.of());
        when(pathService.recommendCityRoute(eq(1L), eq(2L), anyMap(), anyString(), eq(5)))
                .thenReturn(cityRouteResult());

        Map<String, Object> context = service.buildRouteCartContext("从清明上河园开始，火车站结束，途径5个景区的方案");
        String answer = String.valueOf(context.get("answer"));

        verify(pathService).recommendCityRoute(eq(1L), eq(2L), anyMap(), anyString(), eq(5));
        verify(pathService, never()).calculateShortestPath(eq(1L), eq(2L), anyString());
        assertThat(answer).contains("本次共安排5个景区")
                .contains("清明上河园")
                .contains("龙亭景区")
                .contains("开封府")
                .contains("大相国寺")
                .contains("铁塔公园")
                .contains("开封站");
    }

    @Test
    void keepsRailwayStationAsStartAndQingmingAsEndWhenEndPhraseHasMoreStopsAfterIt() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        PathService pathService = mock(PathService.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "pathService", pathService);

        List<LargeScenicArea> areas = List.of(
                area(1L, "开封站", 1),
                area(2L, "清明上河园", 0),
                area(3L, "龙亭景区", 0),
                area(4L, "中国翰园碑林", 0),
                area(5L, "万岁山武侠城", 0),
                area(6L, "天波杨府", 0)
        );
        when(largeRepository.findAll()).thenReturn(areas);
        when(smallRepository.findAll()).thenReturn(List.of());
        when(pathService.recommendCityRoute(eq(1L), eq(2L), anyMap(), anyString(), eq(5)))
                .thenReturn(Map.of(
                        "success", true,
                        "pathDetails", List.of(
                                Map.of("id", 1L, "name", "开封站", "isAreaType", 1),
                                Map.of("id", 3L, "name", "龙亭景区", "isAreaType", 0),
                                Map.of("id", 4L, "name", "中国翰园碑林", "isAreaType", 0),
                                Map.of("id", 5L, "name", "万岁山武侠城", "isAreaType", 0),
                                Map.of("id", 6L, "name", "天波杨府", "isAreaType", 0),
                                Map.of("id", 2L, "name", "清明上河园", "isAreaType", 0)
                        ),
                        "recommendedAreaIds", List.of(1L, 3L, 4L, 5L, 6L, 2L),
                        "recommendedScenicAreaIds", List.of(3L, 4L, 5L, 6L, 2L),
                        "segmentDetails", List.of(),
                        "visitDetails", List.of(),
                        "totalDuration", 0,
                        "overallDuration", 0,
                        "totalCost", 0.0
                ));

        Map<String, Object> context = service.buildRouteCartContext("从火车站开始，到清明上河园结束途径5处景区的方案");
        String answer = String.valueOf(context.get("answer"));

        verify(pathService).recommendCityRoute(eq(1L), eq(2L), anyMap(), anyString(), eq(5));
        assertThat(answer)
                .contains("起点偏好：开封站")
                .contains("终点偏好：清明上河园")
                .doesNotContain("起点偏好：清明上河园");
    }

    @Test
    void ignoresGenericScenicCategoryQueries() {
        RagService service = new RagService();
        LinkedHashSet<String> allowedNames = new LinkedHashSet<>(List.of("清明上河园"));

        Boolean unrecorded = ReflectionTestUtils.invokeMethod(
                service,
                "isUnrecordedSpecificScenicQuery",
                "开封有哪些楼值得去",
                allowedNames
        );

        assertThat(unrecorded).isFalse();
    }

    @Test
    void keepsUnknownScenicFallbackPhraseDuringValidation() {
        RagService service = new RagService();
        LargeScenicArea knownArea = area("清明上河园");
        LinkedHashSet<String> allowedNames = new LinkedHashSet<>(List.of("清明上河园"));

        String answer = ReflectionTestUtils.invokeMethod(
                service,
                "sanitizeAndValidateGeneratedAnswer",
                "开封市并没有这个景点。",
                "白马楼如何游玩",
                List.of(knownArea),
                List.<SmallScenicSpot>of(),
                "",
                allowedNames
        );

        assertThat(answer).isEqualTo("开封市并没有这个景点。");
    }

    @Test
    void usesDatabaseFactAnswerWhenModelIsUnavailable() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        LargeScenicArea area = area(2L, "清明上河园", 0);
        area.setDescription("以宋文化沉浸体验和演艺见长。");
        area.setOpeningHours("09:00-22:00");
        area.setPrice(BigDecimal.valueOf(120));
        when(largeRepository.findAll()).thenReturn(List.of(area));
        when(smallRepository.findAll()).thenReturn(List.of(
                spot(1L, 2L, "虹桥"),
                spot(2L, 2L, "东京码头")
        ));

        String answer = service.generateAnswer("介绍一下清明上河园");

        assertThat(answer)
                .contains("清明上河园")
                .contains("以宋文化沉浸体验和演艺见长")
                .contains("09:00-22:00")
                .contains("门票参考：120.0元")
                .contains("虹桥")
                .contains("东京码头");
    }

    @Test
    void fallsBackToFactAnswerWhenPolishAddsUnknownScenicName() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "chatModel", model);

        LargeScenicArea area = area(2L, "清明上河园", 0);
        area.setDescription("以宋文化沉浸体验和演艺见长。");
        area.setPrice(BigDecimal.valueOf(120));
        when(largeRepository.findAll()).thenReturn(List.of(area));
        when(smallRepository.findAll()).thenReturn(List.of());
        when(model.generate(anyString())).thenReturn("清明上河园适合游玩，也可以顺便去白马楼。");

        String answer = service.generateAnswer("介绍一下清明上河园");

        verify(model, org.mockito.Mockito.atLeastOnce()).generate(anyString());
        assertThat(answer)
                .contains("清明上河园")
                .contains("门票参考：120.0元")
                .doesNotContain("白马楼");
    }

    @Test
    void usesChatMemoryForFollowUpReferenceInSameUserConversation() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ChatLanguageModel model = mock(ChatLanguageModel.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);
        ReflectionTestUtils.setField(service, "chatModel", model);

        LargeScenicArea area = area(2L, "清明上河园", 0);
        area.setDescription("以宋文化沉浸体验和演艺见长。");
        area.setPrice(BigDecimal.valueOf(120));
        when(largeRepository.findAll()).thenReturn(List.of(area));
        when(smallRepository.findAll()).thenReturn(List.of());
        when(model.generate(anyString())).thenReturn(
                "intent=SCENIC_DETAIL\nscenicName=清明上河园\nmaxStops=0\nreason=景区详情",
                "清明上河园是开封的真实景点，门票参考：120.0元。",
                "intent=SCENIC_DETAIL\nscenicName=清明上河园\nmaxStops=0\nreason=承接上文",
                "清明上河园门票参考：120.0元。"
        );

        service.generateAnswer("介绍一下清明上河园", 7L, "alice");
        String followUpAnswer = service.generateAnswer("它的门票多少钱", 7L, "alice");

        assertThat(followUpAnswer)
                .contains("清明上河园")
                .contains("120.0元");
        ArgumentCaptor<String> promptCaptor = ArgumentCaptor.forClass(String.class);
        verify(model, org.mockito.Mockito.atLeast(4)).generate(promptCaptor.capture());
        boolean hasPreviousTurnInPrompt = promptCaptor.getAllValues().stream().anyMatch(prompt ->
                prompt.contains("最近对话")
                        && prompt.contains("用户：介绍一下清明上河园")
                        && prompt.contains("助手：清明上河园是开封的真实景点"));
        assertThat(hasPreviousTurnInPrompt).isTrue();
    }

    @Test
    void recommendsUnseenScenicAreasForMoreFollowUpInSameUserConversation() {
        RagService service = new RagService();
        LargeScenicAreaRepository largeRepository = mock(LargeScenicAreaRepository.class);
        SmallScenicSpotRepository smallRepository = mock(SmallScenicSpotRepository.class);
        ReflectionTestUtils.setField(service, "largeScenicAreaRepository", largeRepository);
        ReflectionTestUtils.setField(service, "smallScenicSpotRepository", smallRepository);

        LargeScenicArea yuwangtai = area(1L, "禹王台公园", 0);
        yuwangtai.setDescription("兼具古迹、园林和休闲属性的历史公园，节奏舒缓，适合轻松游览。");
        LargeScenicArea fanta = area(2L, "繁塔", 0);
        fanta.setDescription("北宋古塔遗存，是开封现存年代很早的重要地面古建筑之一，适合历史向和古建向游客。");
        LargeScenicArea daxiangguosi = area(3L, "大相国寺", 0);
        daxiangguosi.setDescription("开封重要佛教文化景点，节奏相对平缓，适合人文参观与静态游览。");
        LargeScenicArea hanyuan = area(4L, "中国翰园碑林", 0);
        hanyuan.setDescription("集碑刻、书法、园林于一体的人文景区，适合文化游和拍照散步。");
        LargeScenicArea tieta = area(5L, "铁塔景区", 0);
        tieta.setDescription("以千年铁塔和园林环境闻名，适合散步、拍照和轻松游览。");
        LargeScenicArea tianbo = area(6L, "天波杨府", 0);
        tianbo.setDescription("以杨家将文化为主题，兼具园林观赏、历史故事和演艺体验。");
        when(largeRepository.findAll()).thenReturn(List.of(
                yuwangtai,
                fanta,
                daxiangguosi,
                hanyuan,
                tieta,
                tianbo,
                area(7L, "龙亭景区", 0),
                area(8L, "开封府", 0),
                area(9L, "清明上河园", 0)
        ));
        when(smallRepository.findAll()).thenReturn(List.of());

        String firstAnswer = service.generateAnswer("有哪些知名景点", 8L, "bob");
        String followUpAnswer = service.generateAnswer("能推荐更多景区吗", 8L, "bob");

        assertThat(firstAnswer)
                .contains("禹王台公园")
                .contains("天波杨府");
        assertThat(followUpAnswer)
                .contains("除了前面提到的景区")
                .contains("龙亭景区")
                .contains("开封府")
                .contains("清明上河园")
                .doesNotContain("禹王台公园")
                .doesNotContain("繁塔")
                .doesNotContain("大相国寺")
                .doesNotContain("中国翰园碑林")
                .doesNotContain("铁塔景区")
                .doesNotContain("天波杨府");
    }

    private LargeScenicArea area(String name) {
        return area(1L, name, 0);
    }

    private LargeScenicArea area(Long id, String name, int isAreaType) {
        LargeScenicArea area = new LargeScenicArea();
        area.setId(id);
        area.setName(name);
        area.setIsAreaType(isAreaType);
        return area;
    }

    private LargeScenicArea taggedArea(Long id, String name, String tags, int isAreaType) {
        LargeScenicArea area = area(id, name, isAreaType);
        area.setTags(tags);
        return area;
    }

    private SmallScenicSpot spot(String name) {
        return spot(1L, 1L, name);
    }

    private SmallScenicSpot spot(Long largeAreaId, String name) {
        return spot(1L, largeAreaId, name);
    }

    private SmallScenicSpot spot(Long id, Long largeAreaId, String name) {
        SmallScenicSpot spot = new SmallScenicSpot();
        spot.setId(id);
        spot.setLargeAreaId(largeAreaId);
        spot.setName(name);
        return spot;
    }

    private LargeScenicArea foodArea(Long id, String name, String description, String tags, int price) {
        LargeScenicArea area = area(id, name, 1);
        area.setDescription(description);
        area.setTags(tags);
        area.setPrice(BigDecimal.valueOf(price));
        area.setFoodConvenienceScore(BigDecimal.valueOf(5));
        area.setPopularityScore(BigDecimal.valueOf(5));
        return area;
    }

    private Product product(Long id, String name, String description, int price) {
        Product product = new Product();
        product.setId(id);
        product.setName(name);
        product.setDescription(description);
        product.setPrice(BigDecimal.valueOf(price));
        product.setStatus("ON_SALE");
        return product;
    }

    private Map<String, Object> cityRouteResult() {
        return Map.of(
                "success", true,
                "pathDetails", List.of(
                        Map.of("id", 1L, "name", "清明上河园", "isAreaType", 0),
                        Map.of("id", 3L, "name", "龙亭景区", "isAreaType", 0),
                        Map.of("id", 4L, "name", "开封府", "isAreaType", 0),
                        Map.of("id", 5L, "name", "大相国寺", "isAreaType", 0),
                        Map.of("id", 6L, "name", "铁塔公园", "isAreaType", 0),
                        Map.of("id", 2L, "name", "开封站", "isAreaType", 1)
                ),
                "recommendedAreaIds", List.of(1L, 3L, 4L, 5L, 6L, 2L),
                "segmentDetails", List.of(),
                "visitDetails", List.of(),
                "totalDistance", 0.0,
                "totalDuration", 0,
                "totalCost", 0.0
        );
    }
}
