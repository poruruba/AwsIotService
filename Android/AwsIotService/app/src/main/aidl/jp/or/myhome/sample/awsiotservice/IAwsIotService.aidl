// IAwsIotService.aidl
package jp.or.myhome.sample.awsiotservice;

import jp.or.myhome.sample.awsiotservice.IAwsIotServiceListener;

// Declare any non-default types here with import statements

interface IAwsIotService {
    /**
     * Demonstrates some basic types that you can use as parameters
     * and return values in AIDL.
     */
//    void basicTypes(int anInt, long aLong, boolean aBoolean, float aFloat,
//            double aDouble, String aString);
    void publishMessage(String topicName, String message);
    boolean isSubscribed();
    void addListener(IAwsIotServiceListener listener);
    void removeListener(IAwsIotServiceListener listener);
}