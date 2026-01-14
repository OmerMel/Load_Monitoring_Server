package com.finalproject.load_monitoring.bootstrap;

import com.finalproject.load_monitoring.entity.Carriage;
import com.finalproject.load_monitoring.entity.Train;
import com.finalproject.load_monitoring.repository.CarriageRepository;
import com.finalproject.load_monitoring.repository.TrainRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
public class DatabaseSeeder implements CommandLineRunner {

    private final TrainRepository trainRepository;
    private final CarriageRepository carriageRepository;

    // הזרקת ה-Repositories כדי שנוכל לשמור נתונים
    public DatabaseSeeder(TrainRepository trainRepository, CarriageRepository carriageRepository) {
        this.trainRepository = trainRepository;
        this.carriageRepository = carriageRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        // בדיקה אם הדאטהבייס כבר מלא (למקרה שנחליף ל-DB אמיתי בעתיד)
        if (trainRepository.count() > 0) {
            return;
        }

        System.out.println("🌱 Seeding database with dummy data...");

        // --- יצירת רכבת 1: נהריה - תל אביב ---
        Train haifaTrain = new Train();
        haifaTrain.setOriginStation("נהריה");
        haifaTrain.setDestinationStation("תל אביב - סבידור");
        haifaTrain.setDepartureTime(LocalDateTime.of(2025, 2, 5, 8, 0));
        haifaTrain.setArrivalTime(LocalDateTime.of(2025, 2, 5, 11, 0));
        haifaTrain.setCurrentStation("Haifa Center");
        haifaTrain.setLastUpdated(LocalDateTime.now());

        // חייבים לשמור את הרכבת קודם כדי שיהיה לה ID
        trainRepository.save(haifaTrain);

        // יצירת קרונות לרכבת 1
        createCarriage(haifaTrain, 1, 80, 82); // קרון ריק
        createCarriage(haifaTrain, 2, 80, 40); // קרון חצי מלא
        createCarriage(haifaTrain, 3, 80, 75); // קרון מלא
        createCarriage(haifaTrain, 4, 80, 82); // קרון עמוס מאוד
        /// //////////////////////////////////////////////////////////////////////////////////////////
        // --- יצירת רכבת 2: נהריה - תל אביב סבידור ---
        Train nahariyaTrain = new Train();
        nahariyaTrain.setOriginStation("נהריה");
        nahariyaTrain.setDestinationStation("תל אביב - סבידור");
        nahariyaTrain.setDepartureTime(LocalDateTime.of(2025, 2, 5, 9, 15));
        nahariyaTrain.setArrivalTime(LocalDateTime.of(2025, 2, 5, 12, 0));
        nahariyaTrain.setCurrentStation("עכו");
        nahariyaTrain.setLastUpdated(LocalDateTime.now());

        trainRepository.save(nahariyaTrain);

        createCarriage(nahariyaTrain, 1, 80, 20);
        createCarriage(nahariyaTrain, 2, 80, 55);
        createCarriage(nahariyaTrain, 3, 80, 78);
        createCarriage(nahariyaTrain, 4, 80, 80);
        //////////////////////////////////////////////////////////////////////////////////////////////
        // --- יצירת רכבת 3: תל אביב אוניברסיטה - נתניה מערב ---
        Train netanyaTrain = new Train();
        netanyaTrain.setOriginStation("תל אביב - אוניברסיטה");
        netanyaTrain.setDestinationStation("נתניה - מערב");
        netanyaTrain.setDepartureTime(LocalDateTime.of(2025, 2, 5, 10, 0));
        netanyaTrain.setArrivalTime(LocalDateTime.of(2025, 2, 5, 10, 45));
        netanyaTrain.setCurrentStation("הרצליה");
        netanyaTrain.setLastUpdated(LocalDateTime.now());

        trainRepository.save(netanyaTrain);

        createCarriage(netanyaTrain, 1, 70, 30);
        createCarriage(netanyaTrain, 2, 70, 60);
        createCarriage(netanyaTrain, 3, 70, 68);

        //////////////////////////////////////////////////////////////////////////////////////////////

//        // --- יצירת רכבת 4: באר שבע - תל אביב ---
//        Train beershebaTrain = new Train();
//        beershebaTrain.setOriginStation("Beersheba North");
//        beershebaTrain.setDestinationStation("Tel Aviv HaShalom");
//        beershebaTrain.setDepartureTime(LocalDateTime.of(2025, 2, 8, 11, 0));
//        beershebaTrain.setArrivalTime(LocalDateTime.of(2025, 2, 8, 12, 30));
//        beershebaTrain.setCurrentStation("Kiryat Gat");
//        beershebaTrain.setLastUpdated(LocalDateTime.now());
//
//        trainRepository.save(beershebaTrain);
//
//        createCarriage(beershebaTrain, 1, 100, 20);
//        createCarriage(beershebaTrain, 2, 100, 50);
        //////////////////////////////////////////////////////////////////////////////////////////////

        System.out.println("✅ Database seeding completed successfully!");
    }

    // פונקציית עזר ליצירת קרון ושמירתו
    private void createCarriage(Train train, int number, int capacity, int currentOccupancy) {
        Carriage c = new Carriage();
        c.setTrain(train);
        c.setCarriageNumber(number);
        c.setMaxCapacity(capacity);
        c.setOccupancy(currentOccupancy);
        c.setLastUpdated(LocalDateTime.now());
        carriageRepository.save(c);
    }
}