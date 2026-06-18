package com.carbonfootprint.platform.carbon.calculation.adapter.out.firestore;

import com.carbonfootprint.platform.activity.model.ActivityCategory;
import com.carbonfootprint.platform.carbon.calculation.model.EmissionFactor;
import com.carbonfootprint.platform.ingestion.enrichment.model.FuelType;
import com.carbonfootprint.platform.ingestion.enrichment.model.TransportMode;
import com.carbonfootprint.platform.shared.constant.ApiConstants;
import com.google.cloud.firestore.CollectionReference;
import com.google.cloud.firestore.DocumentReference;
import com.google.cloud.firestore.Firestore;
import com.google.cloud.firestore.Query;
import com.google.cloud.firestore.QuerySnapshot;
import com.google.api.core.ApiFuture;
import com.google.cloud.firestore.WriteResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmissionFactorSeederTest {

    @Mock
    private Firestore firestore;

    @Mock
    private CollectionReference collectionRef;

    @Mock
    private Query query;

    @Mock
    private ApiFuture<QuerySnapshot> queryFuture;

    @Mock
    private QuerySnapshot querySnapshot;

    @Mock
    private DocumentReference documentRef;

    @Mock
    private ApiFuture<WriteResult> writeFuture;

    private EmissionFactorSeeder seeder;

    @BeforeEach
    void setUp() {
        seeder = new EmissionFactorSeeder(firestore);
        lenient().when(firestore.collection(ApiConstants.COLLECTION_EMISSION_FACTORS))
                .thenReturn(collectionRef);
    }

    // ── Empty collection → seeds ──────────────────────────────────────────

    @Test
    void run_emptyCollection_seedsAllFactors() throws Exception {
        // Simulate empty collection on check
        when(collectionRef.limit(1)).thenReturn(query);
        when(query.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);

        // Simulate successful writes
        when(collectionRef.document(anyString())).thenReturn(documentRef);
        when(documentRef.set(any())).thenReturn(writeFuture);
        when(writeFuture.get()).thenReturn(null);

        seeder.run();

        // Verify documents were created for each default factor
        ArgumentCaptor<String> docIdCaptor = ArgumentCaptor.forClass(String.class);
        verify(collectionRef, times(12)).document(docIdCaptor.capture());

        List<String> docIds = docIdCaptor.getAllValues();
        assertThat(docIds).containsExactlyInAnyOrder(
                "ef-fuel-petrol", "ef-fuel-diesel", "ef-fuel-lpg", "ef-fuel-cng",
                "ef-electricity-grid",
                "ef-flight-economy", "ef-flight-business",
                "ef-transport-bus", "ef-transport-metro", "ef-transport-train", "ef-transport-taxi",
                "ef-shopping-default"
        );
    }

    @Test
    void run_emptyCollection_writesCorrectData() throws Exception {
        when(collectionRef.limit(1)).thenReturn(query);
        when(query.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(true);

        when(collectionRef.document(anyString())).thenReturn(documentRef);
        when(documentRef.set(any())).thenReturn(writeFuture);
        when(writeFuture.get()).thenReturn(null);

        seeder.run();

        // Verify all 12 factors were written
        ArgumentCaptor<Map<String, Object>> dataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(documentRef, times(12)).set(dataCaptor.capture());

        List<Map<String, Object>> allWrites = dataCaptor.getAllValues();
        assertThat(allWrites).hasSize(12);

        // Verify petrol factor was written with correct value
        assertThat(allWrites.get(0).get("id")).isEqualTo("ef-fuel-petrol");
        assertThat(allWrites.get(0).get("category")).isEqualTo("FUEL");
        assertThat(allWrites.get(0).get("fuelType")).isEqualTo("PETROL");
        assertThat(allWrites.get(0).get("value")).isEqualTo(2.31);
    }

    // ── Non-empty collection → skips ──────────────────────────────────────

    @Test
    void run_collectionHasData_skipsSeeding() throws Exception {
        when(collectionRef.limit(1)).thenReturn(query);
        when(query.get()).thenReturn(queryFuture);
        when(queryFuture.get()).thenReturn(querySnapshot);
        when(querySnapshot.isEmpty()).thenReturn(false);

        seeder.run();

        // No writes should happen
        verify(collectionRef, never()).document(anyString());
    }

    // ── Default factors validation ────────────────────────────────────────

    @Test
    void defaultEmissionFactors_containsAllRequiredCategories() {
        List<EmissionFactor> defaults = EmissionFactorSeeder.defaultEmissionFactors();

        assertThat(defaults).hasSize(12);

        // Verify all categories are represented
        assertThat(defaults).anyMatch(f -> f.getCategory() == ActivityCategory.FUEL);
        assertThat(defaults).anyMatch(f -> f.getCategory() == ActivityCategory.ELECTRICITY);
        assertThat(defaults).anyMatch(f -> f.getCategory() == ActivityCategory.FLIGHT);
        assertThat(defaults).anyMatch(f -> f.getCategory() == ActivityCategory.TRANSPORT);
        assertThat(defaults).anyMatch(f -> f.getCategory() == ActivityCategory.SHOPPING);
    }

    @Test
    void defaultEmissionFactors_fuel_hasAllFuelTypes() {
        List<EmissionFactor> defaults = EmissionFactorSeeder.defaultEmissionFactors();
        List<EmissionFactor> fuelFactors = defaults.stream()
                .filter(f -> f.getCategory() == ActivityCategory.FUEL)
                .toList();

        assertThat(fuelFactors).hasSize(4);
        assertThat(fuelFactors).anyMatch(f -> f.getFuelType().orElse(null) == FuelType.PETROL);
        assertThat(fuelFactors).anyMatch(f -> f.getFuelType().orElse(null) == FuelType.DIESEL);
        assertThat(fuelFactors).anyMatch(f -> f.getFuelType().orElse(null) == FuelType.LPG);
        assertThat(fuelFactors).anyMatch(f -> f.getFuelType().orElse(null) == FuelType.CNG);
    }

    @Test
    void defaultEmissionFactors_transport_hasAllModes() {
        List<EmissionFactor> defaults = EmissionFactorSeeder.defaultEmissionFactors();
        List<EmissionFactor> transportFactors = defaults.stream()
                .filter(f -> f.getCategory() == ActivityCategory.TRANSPORT)
                .toList();

        assertThat(transportFactors).hasSize(4);
        assertThat(transportFactors).anyMatch(f -> f.getTransportMode().orElse(null) == TransportMode.BUS);
        assertThat(transportFactors).anyMatch(f -> f.getTransportMode().orElse(null) == TransportMode.METRO);
        assertThat(transportFactors).anyMatch(f -> f.getTransportMode().orElse(null) == TransportMode.TRAIN);
        assertThat(transportFactors).anyMatch(f -> f.getTransportMode().orElse(null) == TransportMode.TAXI);
    }

    @Test
    void defaultEmissionFactors_flight_hasEconomyAndBusiness() {
        List<EmissionFactor> defaults = EmissionFactorSeeder.defaultEmissionFactors();
        List<EmissionFactor> flightFactors = defaults.stream()
                .filter(f -> f.getCategory() == ActivityCategory.FLIGHT)
                .toList();

        assertThat(flightFactors).hasSize(2);
        assertThat(flightFactors).anyMatch(f -> f.getId().equals("ef-flight-economy"));
        assertThat(flightFactors).anyMatch(f -> f.getId().equals("ef-flight-business"));
    }

    @Test
    void defaultEmissionFactors_allHavePositiveValues() {
        List<EmissionFactor> defaults = EmissionFactorSeeder.defaultEmissionFactors();

        assertThat(defaults).allMatch(f -> f.getValue().compareTo(BigDecimal.ZERO) > 0);
    }

    @Test
    void defaultEmissionFactors_allHaveNonNullFields() {
        List<EmissionFactor> defaults = EmissionFactorSeeder.defaultEmissionFactors();

        assertThat(defaults).allMatch(f -> f.getId() != null && !f.getId().isBlank());
        assertThat(defaults).allMatch(f -> f.getUnit() != null && !f.getUnit().isBlank());
        assertThat(defaults).allMatch(f -> f.getSource() != null && !f.getSource().isBlank());
        assertThat(defaults).allMatch(f -> f.getVersion() != null && !f.getVersion().isBlank());
        assertThat(defaults).allMatch(f -> f.getMethodName() != null && !f.getMethodName().isBlank());
        assertThat(defaults).allMatch(f -> f.getValidFrom() != null);
    }

    @Test
    void defaultEmissionFactors_uniqueIds() {
        List<EmissionFactor> defaults = EmissionFactorSeeder.defaultEmissionFactors();
        List<String> ids = defaults.stream().map(EmissionFactor::getId).toList();

        assertThat(ids).hasSameSizeAs(java.util.Set.copyOf(ids));
    }

    // ── toFirestoreMap ────────────────────────────────────────────────────

    @Test
    void toFirestoreMap_mapsAllFields() {
        EmissionFactor factor = EmissionFactor.builder()
                .id("test-id")
                .category(ActivityCategory.FUEL)
                .fuelType(FuelType.PETROL)
                .transportMode(null)
                .region("IN")
                .value(new BigDecimal("2.31"))
                .unit("litre")
                .source("DEFRA 2024")
                .version("2024-v1")
                .methodName("Tier 1")
                .validFrom(java.time.Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        Map<String, Object> map = EmissionFactorSeeder.toFirestoreMap(factor);

        assertThat(map.get("id")).isEqualTo("test-id");
        assertThat(map.get("category")).isEqualTo("FUEL");
        assertThat(map.get("fuelType")).isEqualTo("PETROL");
        assertThat(map.get("transportMode")).isNull();
        assertThat(map.get("region")).isEqualTo("IN");
        assertThat(map.get("value")).isEqualTo(2.31);
        assertThat(map.get("unit")).isEqualTo("litre");
        assertThat(map.get("source")).isEqualTo("DEFRA 2024");
        assertThat(map.get("version")).isEqualTo("2024-v1");
        assertThat(map.get("methodName")).isEqualTo("Tier 1");
        assertThat(map.get("validFrom")).isEqualTo("2024-01-01T00:00:00Z");
        assertThat(map.get("validTo")).isNull();
    }

    @Test
    void toFirestoreMap_nullFuelTypeAndTransportMode_mapsNulls() {
        EmissionFactor factor = EmissionFactor.builder()
                .id("test-id")
                .category(ActivityCategory.SHOPPING)
                .value(new BigDecimal("0.001"))
                .unit("INR")
                .source("TEST")
                .version("v1")
                .methodName("Test")
                .validFrom(java.time.Instant.parse("2024-01-01T00:00:00Z"))
                .build();

        Map<String, Object> map = EmissionFactorSeeder.toFirestoreMap(factor);

        assertThat(map.get("fuelType")).isNull();
        assertThat(map.get("transportMode")).isNull();
        assertThat(map.get("region")).isNull();
    }

    @Test
    void toFirestoreMap_withValidTo_includesValidTo() {
        EmissionFactor factor = EmissionFactor.builder()
                .id("test-id")
                .category(ActivityCategory.SHOPPING)
                .value(new BigDecimal("0.001"))
                .unit("INR")
                .source("TEST")
                .version("v1")
                .methodName("Test")
                .validFrom(java.time.Instant.parse("2024-01-01T00:00:00Z"))
                .validTo(java.time.Instant.parse("2025-12-31T23:59:59Z"))
                .build();

        Map<String, Object> map = EmissionFactorSeeder.toFirestoreMap(factor);

        assertThat(map.get("validTo")).isEqualTo("2025-12-31T23:59:59Z");
    }
}
