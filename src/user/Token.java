package cn.dreampie.function.user;

import cn.dreampie.common.web.model.Model;
import com.jfinal.ext.plugin.tablebind.TableBind;

/**
 * Created by wangrenhui on 14-4-17.
 */
@TableBind(tableName = "sec_token", pkName = "uuid")
public class Token extends Model<Token> {
    public static Token dao = new Token();

}