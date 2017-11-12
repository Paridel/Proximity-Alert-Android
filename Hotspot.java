package cg.paridel.mazone.cache;

import android.os.Parcel;
import android.os.Parcelable;

/**
 * Created by Paridel MAKOUALA on 11/10/2017.
 */

public class Hotspot implements Parcelable {

    private int idClient;
    private String nomClient, linkClient, latClient, longClient, cityClient;

    public Hotspot() {

    }

    public Hotspot(String nomClient, String linkClient, String latClient, String longClient, String cityClient) {
        this.nomClient = nomClient;
        this.linkClient = linkClient;
        this.latClient = latClient;
        this.longClient = longClient;
        this.cityClient = cityClient;
    }

    public Hotspot(int idClient, String nomClient, String linkClient, String latClient, String longClient, String cityClient) {
        this.idClient = idClient;
        this.nomClient = nomClient;
        this.linkClient = linkClient;
        this.latClient = latClient;
        this.longClient = longClient;
        this.cityClient = cityClient;
    }

    protected Hotspot(Parcel in) {
        idClient = in.readInt();
        nomClient = in.readString();
        linkClient = in.readString();
        latClient = in.readString();
        longClient = in.readString();
        cityClient = in.readString();
    }

    public static final Creator<Hotspot> CREATOR = new Creator<Hotspot>() {
        @Override
        public Hotspot createFromParcel(Parcel in) {
            return new Hotspot(in);
        }

        @Override
        public Hotspot[] newArray(int size) {
            return new Hotspot[size];
        }
    };

    public int getIdClient() {
        return idClient;
    }

    public void setIdClient(int idClient) {
        this.idClient = idClient;
    }

    public String getNomClient() {
        return nomClient;
    }

    public void setNomClient(String nomClient) {
        this.nomClient = nomClient;
    }

    public String getLinkClient() {
        return linkClient;
    }

    public void setLinkClient(String linkClient) {
        this.linkClient = linkClient;
    }

    public String getLatClient() {
        return latClient;
    }

    public void setLatClient(String latClient) {
        this.latClient = latClient;
    }

    public String getLongClient() {
        return longClient;
    }

    public void setLongClient(String longClient) {
        this.longClient = longClient;
    }

    public String getCityClient() {
        return cityClient;
    }

    public void setCityClient(String cityClient) {
        this.cityClient = cityClient;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel parcel, int i) {
        parcel.writeInt(idClient);
        parcel.writeString(nomClient);
        parcel.writeString(linkClient);
        parcel.writeString(latClient);
        parcel.writeString(longClient);
        parcel.writeString(cityClient);
    }
}
