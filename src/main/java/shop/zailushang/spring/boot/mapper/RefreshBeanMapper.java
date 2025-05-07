package shop.zailushang.spring.boot.mapper;

import org.apache.ibatis.annotations.*;
import shop.zailushang.spring.boot.model.RefreshBeanModel;

import java.util.List;
import java.util.Optional;

public interface RefreshBeanMapper {
    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "bean_name", javaType = String.class),
            @Arg(column = "lambda_script", javaType = String.class),
            @Arg(column = "description", javaType = String.class)
    })
    @Select("select * from refresh_bean")
    List<RefreshBeanModel> selectAll();

    @ConstructorArgs({
            @Arg(column = "id", javaType = Long.class),
            @Arg(column = "bean_name", javaType = String.class),
            @Arg(column = "lambda_script", javaType = String.class),
            @Arg(column = "description", javaType = String.class)
    })
    @Select("select * from refresh_bean where bean_name = #{beanName}")
    Optional<RefreshBeanModel> selectOne(@Param("beanName") String beanName);


    @Insert("""
            insert into refresh_bean(
                bean_name,
                lambda_script,
                description
            ) values (
                #{refreshBeanModel.beanName},
                #{refreshBeanModel.lambdaScript},
                #{refreshBeanModel.description}
            )
            """)
    int insert(@Param("refreshBeanModel") RefreshBeanModel refreshBeanModel);

    @Update("""
            <script>
                update refresh_bean
                <set>
                    <if test="refreshBeanModel.lambdaScript != null and refreshBeanModel.lambdaScript != ''">
                        lambda_script = #{refreshBeanModel.lambdaScript},
                    </if>
                    <if test="refreshBeanModel.description != null and refreshBeanModel.description != ''">
                        description = #{refreshBeanModel.description},
                    </if>
                </set>
                where bean_name = #{refreshBeanModel.beanName}
            </script>
            """)
    int update(@Param("refreshBeanModel") RefreshBeanModel refreshBeanModel);

    @Delete("delete from refresh_bean where bean_name = #{beanName}")
    int delete(@Param("beanName") String beanName);
}