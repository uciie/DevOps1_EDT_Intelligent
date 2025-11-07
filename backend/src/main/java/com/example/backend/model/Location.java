package com.example.backend.model;

import jakarta.persistence.*;

/**
 * Représente un lieu géographique pour un événement ou une tâche.
 * Contient l'adresse et les coordonnées GPS pour le calcul des trajets.
 */
@Entity
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Adresse complète du lieu, y compris rue, ville, code postal, pays
    @Column(nullable = false)
    private String address;

    private Double latitude;
    private Double longitude;

    // Nom optionnel du lieu (ex: "Bureau", "Domicile")
    private String name;

    /**
     * Constructeur par défaut.
     */
    public Location() {}

    /**
     * Construit un nouveau lieu avec une adresse.
     *
     * @param address l'adresse du lieu
     */
    public Location(String address) {
        this.address = address;
    }

    /**
     * Construit un nouveau lieu avec adresse et coordonnées GPS.
     *
     * @param address l'adresse du lieu
     * @param latitude la latitude
     * @param longitude la longitude
     */
    public Location(String address, Double latitude, Double longitude) {
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    // Getters et Setters

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getAddress() {
        return address;
    }

    public void setAddress(String address) {
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    /**
     * Vérifie si le lieu possède des coordonnées GPS valides.
     *
     * @return true si latitude et longitude sont définies
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }
}
