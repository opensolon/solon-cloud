/*
 * Copyright 2017-2024 noear.org and authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.noear.solon.cloud.extend.aliyun.oss.service;

import java.io.Serializable;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;

/**
 * @author noear
 * @since 1.3
 */

class Datetime implements Serializable,Cloneable {
    private Date _datetime;
    private Calendar _calendar = null;

    public Datetime(Date date){
        _datetime = date;
        _calendar = Calendar.getInstance();
        _calendar.setTime(date);
    }

    //当前时间
    ////@XNote("当前时间")
    public static Datetime Now(){
        return new Datetime(new Date());
    }


    @Override
    //@XNote("转为字符串")
    public String toString(){
        return toString("yyyy-MM-dd HH:mm:ss");
    }

    //@XNote("转为GMT格式字符串")
    public String toGmtString() {
        return toString("EEE, dd MMM yyyy HH:mm:ss 'GMT'",
                Locale.US,
                TimeZone.getTimeZone("GMT"));

    }

    //转成String
    //@XNote("格式化为字符串")
    public String toString(String format){
        DateFormat df = new SimpleDateFormat(format);
        return df.format(_datetime);
    }

    //转成String
    //@XNote("格式化为字符串")
    public String toString(String format, Locale locale, TimeZone timeZone) {
        DateFormat df = null;
        if (locale == null) {
            df = new SimpleDateFormat(format);
        } else {
            df = new SimpleDateFormat(format, locale);
        }

        if (timeZone != null) {
            df.setTimeZone(timeZone);
        }

        return df.format(_datetime);
    }
}