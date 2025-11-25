package com.example.backend.model;

import jakarta.persistence.*;

/**
 * Représente un lieu géographique pour un événement ou une tâche.
 * 
 * NOUVELLES FONCTIONNALITÉS :
 * - Auto-complétion : L'adresse OU les coordonnées suffisent
 * - Si vous fournissez l'adresse → Les coordonnées GPS sont calculées automatiquement
 * - Si vous fournissez les GPS → L'adresse est récupérée automatiquement
 * 
 * UTILISATION RECOMMANDÉE :
 * 1. Avec GeocodingService (auto-complétion activée) :
 *    Location loc = geocodingService.createLocationFromAddress("5 Avenue Anatole France, 75007 Paris, France");
 *    // → Aura automatiquement les coordonnées GPS !
 * 
 * 2. Sans GeocodingService (mode simple) :
 *    Location loc = new Location("5 Avenue Anatole France, 75007 Paris, France");
 *    // → Fonctionne mais sans GPS automatique
 */
@Entity
public class Location {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Adresse COMPLÈTE du lieu : [numéro] [rue], [code postal] [ville], [pays]
     * 
     * Exemples valides :
     * - "5 Avenue Anatole France, 75007 Paris, France"
     * - "8 Place de Fourvière, 69005 Lyon, France"
     * - "Place Bellecour, 69002 Lyon, France"
     * 
     * ⚠️  NE PAS inclure le nom du monument/lieu dans l'adresse.
     * Utiliser le champ 'name' pour ça.
     */
    @Column(name = "address", nullable = false)
    private String address;

    /**
     * Latitude GPS (optionnelle mais FORTEMENT recommandée).
     * Permet d'éviter toute ambiguïté d'adresse.
     * 
     * 💡 Si non fournie, peut être calculée automatiquement via GeocodingService
     */
    @Column(name = "latitude")
    private Double latitude;

    /**
     * Longitude GPS (optionnelle mais FORTEMENT recommandée).
     * Permet d'éviter toute ambiguïté d'adresse.
     * 
     * 💡 Si non fournie, peut être calculée automatiquement via GeocodingService
     */
    @Column(name = "longitude")
    private Double longitude;

    /**
     * Nom optionnel du lieu pour affichage utilisateur.
     * 
     * Exemples :
     * - "Tour Eiffel"
     * - "Basilique de Fourvière"
     * - "Bureau"
     * - "Domicile"
     * 
     * Ce champ est UNIQUEMENT pour l'affichage.
     * Il N'EST PAS utilisé pour le calcul de trajet.
     */
    @Column(length = 200)
    private String name;

    /**
     * Indique si cette Location a été auto-complétée par géocodage.
     * Utile pour le débogage et la traçabilité.
     */
    @Transient
    private boolean autoCompleted = false;


    /**
     * Constructeur par défaut (requis par JPA).
     */
    public Location() {}

    /**
     * Construit un lieu avec une adresse complète uniquement.
     * 
     * @param address adresse complète du lieu (rue, code postal, ville, pays)
     */
    public Location(String address) {
        validateAddress(address);
        this.address = address;
    }

    /**
     * Construit un lieu avec adresse ET coordonnées GPS.
     * C'est le constructeur le plus complet et recommandé.
     * 
     * @param address adresse complète
     * @param latitude latitude GPS
     * @param longitude longitude GPS
     */
    public Location(String address, Double latitude, Double longitude) {
        validateAddress(address);
        validateCoordinates(latitude, longitude);
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Construit un lieu avec coordonnées GPS uniquement.
     * 
     * @param latitude latitude GPS
     * @param longitude longitude GPS
     */
    public Location(Double latitude, Double longitude) {
        validateCoordinates(latitude, longitude);
        this.latitude = latitude;
        this.longitude = longitude;
    }

    /**
     * Valide que l'adresse est bien formatée.
     * 
     * @param address l'adresse à valider
     * @throws IllegalArgumentException si l'adresse est invalide
     */
    private void validateAddress(String address) {
        if (address == null || address.trim().isEmpty()) {
            throw new IllegalArgumentException("L'adresse ne peut pas être vide");
        }

        String trimmedAddress = address.trim();

        // Vérifier longueur minimale (au moins ville + pays)
        if (trimmedAddress.length() < 10) {
            throw new IllegalArgumentException(
                "L'adresse est trop courte. Format attendu : [rue], [code postal] [ville], [pays]"
            );
        }

        // Vérifier présence d'une virgule (séparateur minimum)
        if (!trimmedAddress.contains(",")) {
            throw new IllegalArgumentException(
                "L'adresse doit contenir au moins une virgule pour séparer ville et pays. " +
                "Format attendu : [rue], [ville], [pays]"
            );
        }

        // Avertissement si l'adresse semble contenir un nom de monument
        String addressLower = trimmedAddress.toLowerCase();
        if (addressLower.contains("tour eiffel") || 
            addressLower.contains("notre-dame") ||
            addressLower.contains("arc de triomphe") ||
            addressLower.contains("basilique") ||
            addressLower.contains("cathédrale")) {
            
            System.err.println("⚠️  WARNING: L'adresse semble contenir un nom de monument.");
            System.err.println("   Adresse: " + address);
            System.err.println("   Conseil: Mettre le nom du monument dans le champ 'name' et garder uniquement l'adresse postale.");
        }
    }

    /**
     * Valide les coordonnées GPS.
     * 
     * @param latitude la latitude
     * @param longitude la longitude
     * @throws IllegalArgumentException si les coordonnées sont invalides
     */
    private void validateCoordinates(Double latitude, Double longitude) {
        if (latitude == null || longitude == null) {
            return; // GPS optionnel
        }

        // Valider latitude (-90 à 90)
        if (latitude < -90.0 || latitude > 90.0) {
            throw new IllegalArgumentException(
                "Latitude invalide: " + latitude + ". Doit être entre -90 et 90"
            );
        }

        // Valider longitude (-180 à 180)
        if (longitude < -180.0 || longitude > 180.0) {
            throw new IllegalArgumentException(
                "Longitude invalide: " + longitude + ". Doit être entre -180 et 180"
            );
        }
    }

    // ========================================================================
    // Getters et Setters
    // ========================================================================

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
        validateAddress(address);
        this.address = address;
    }

    public Double getLatitude() {
        return latitude;
    }

    public void setLatitude(Double latitude) {
        validateCoordinates(latitude, this.longitude);
        this.latitude = latitude;
    }

    public Double getLongitude() {
        return longitude;
    }

    public void setLongitude(Double longitude) {
        validateCoordinates(this.latitude, longitude);
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public boolean isAutoCompleted() {
        return autoCompleted;
    }

    public void setAutoCompleted(boolean autoCompleted) {
        this.autoCompleted = autoCompleted;
    }

    /**
     * Vérifie si le lieu possède des coordonnées GPS valides.
     * 
     * @return true si latitude et longitude sont définies et valides
     */
    public boolean hasCoordinates() {
        return latitude != null && longitude != null;
    }

    /**
     * Vérifie si l'adresse est une adresse GPS générée automatiquement.
     * 
     * @return true si l'adresse commence par "GPS:"
     */
    public boolean hasGeneratedAddress() {
        return address != null && address.startsWith("GPS:");
    }

    /**
     * Retourne une représentation lisible du lieu pour l'affichage.
     * 
     * @return nom si défini, sinon adresse
     */
    public String getDisplayName() {
        if (name != null && !name.trim().isEmpty()) {
            return name;
        }
        // Retourner une version courte de l'adresse (jusqu'à la première virgule)
        if (address != null && !hasGeneratedAddress()) {
            int commaIndex = address.indexOf(',');
            if (commaIndex > 0) {
                return address.substring(0, commaIndex);
            }
            return address;
        }
        if (hasCoordinates()) {
            return String.format("GPS: %.4f, %.4f", latitude, longitude);
        }
        return "Unknown Location";
    }

    /**
     * Retourne une description complète du lieu.
     * 
     * @return description avec nom, adresse et GPS si disponibles
     */
    public String getFullDescription() {
        StringBuilder sb = new StringBuilder();
        
        if (name != null && !name.trim().isEmpty()) {
            sb.append(name).append("\n");
        }
        
        if (address != null && !hasGeneratedAddress()) {
            sb.append("📍 ").append(address);
        }
        
        if (hasCoordinates()) {
            if (sb.length() > 0) sb.append("\n");
            sb.append("GPS: ").append(latitude).append(", ").append(longitude);
        }
        
        if (autoCompleted) {
            sb.append(" ✓");
        }
        
        return sb.toString();
    }

    @Override
    public String toString() {
        return "Location{" +
                "id=" + id +
                ", name='" + name + '\'' +
                ", address='" + address + '\'' +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                ", autoCompleted=" + autoCompleted +
                '}';
    }

    @Override
    public int hashCode() {
        int result = id != null ? id.hashCode() : 0;
        result = 31 * result + (address != null ? address.hashCode() : 0);
        result = 31 * result + (latitude != null ? latitude.hashCode() : 0);
        result = 31 * result + (longitude != null ? longitude.hashCode() : 0);
        return result;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Location location = (Location) o;

        if (id != null ? !id.equals(location.id) : location.id != null) return false;
        if (address != null ? !address.equals(location.address) : location.address != null) return false;
        if (latitude != null ? !latitude.equals(location.latitude) : location.latitude != null) return false;
        return longitude != null ? longitude.equals(location.longitude) : location.longitude == null;
    }
}