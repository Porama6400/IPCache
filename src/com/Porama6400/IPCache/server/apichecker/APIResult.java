package com.Porama6400.IPCache.server.apichecker;

public class APIResult {
    private QueryResult result;
    private ResultSource source = ResultSource.UNKNOWN;
    private String IP;
    private String ASN;
    private String ISP;
    private String Country;
    private String clientType;
    private boolean VPN;

    public APIResult() {
    }

    public ResultSource getSource() {
        return source;
    }

    public void setSource(ResultSource source) {
        this.source = source;
    }

    public QueryResult getResult() {
        return result;
    }

    public void setResult(QueryResult result) {
        this.result = result;
    }

    public String getIP() {
        return IP;
    }

    public void setIP(String IP) {
        this.IP = IP;
    }

    public String getISP() {
        return ISP;
    }

    public void setISP(String ISP) {
        this.ISP = ISP;
    }

    public String getASN() {
        return ASN;
    }

    public void setASN(String ASN) {
        this.ASN = ASN;
    }

    public String getCountry() {
        return Country;
    }

    public void setCountry(String country) {
        Country = country;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public boolean isVPN() {
        return VPN;
    }

    public void setVPN(boolean VPN) {
        this.VPN = VPN;
    }

    public enum QueryResult {
        OK,
        DENIED,
        ERROR,
        LOAD_BALANCER_ERROR,
    }

    public enum ResultSource {
        EXTERNAL_API,
        RAM_CACHE,
        DATABASE_CACHE,
        UNKNOWN
    }
}
