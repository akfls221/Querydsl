package study.querydsl;

import com.querydsl.core.QueryResults;
import com.querydsl.core.Tuple;
import com.querydsl.core.types.ExpressionUtils;
import com.querydsl.core.types.Projections;
import com.querydsl.core.types.dsl.CaseBuilder;
import com.querydsl.core.types.dsl.Expressions;
import com.querydsl.jpa.JPAExpressions;
import com.querydsl.jpa.impl.JPAQueryFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Commit;
import org.springframework.transaction.annotation.Transactional;
import study.querydsl.dto.MemberDto;
import study.querydsl.dto.UserDto;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;
import study.querydsl.entity.Team;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.PersistenceUnit;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static study.querydsl.entity.QMember.member;
import static study.querydsl.entity.QTeam.*;

@SpringBootTest
@Transactional
@Commit
public class QuerydslBasickTest {

    @Autowired
    EntityManager em;

    JPAQueryFactory queryFactory;


    @BeforeEach
    public void before() {
        queryFactory = new JPAQueryFactory(em);
        Team teamA = new Team("teamA");
        Team teamB = new Team("teamB");
        em.persist(teamA);
        em.persist(teamB);

        Member member1 = new Member("member1", 10, teamA);
        Member member2 = new Member("member2", 20, teamA);

        Member member3 = new Member("member3", 30, teamB);
        Member member4 = new Member("member4", 40, teamB);
        em.persist(member1);
        em.persist(member2);
        em.persist(member3);
        em.persist(member4);
    }

    @Test
    public void startJPQL() {
        //member1을 찾아라.
        Member result = em.createQuery("select m from Member m where m.username = :username", Member.class)
                .setParameter("username", "member1")
                .getSingleResult();

        assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    public void startQuerydsl() {
        QMember m = new QMember("m");

        Member result = queryFactory
                .select(m)
                .from(m)
                .where(m.username.eq("member1"))
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    public  void otherQuerydsl() { //static import 방법
        Member result = queryFactory
                .select(member)
                .from(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search() {
        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.eq(10)))
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");
    }

    @Test
    public void search2() {
        Member result = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1").and(member.age.between(10, 20)))
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");
    }

    @DisplayName("and chain을 ','로 대체 가능하다")
    @Test
    public void searchAndParam() {
        Member result = queryFactory
                .selectFrom(member)
                .where(
                        member.username.eq("member1"), member.age.between(10, 20)
                )
                .fetchOne();

        assertThat(result.getUsername()).isEqualTo("member1");
    }

    /**
     * fetch() : 리스트 조회, 데이터 없으면 빈 리스트 반환
     * fetchOne() : 단 건 조회, 결과 없으면 'null', 결과 둘 이상이면 'com.querydsl.core.NonUniqueResultException'
     * fetchFirst() : limit(1).fetchOne()
     * fetchResults() : 페이징 정보 포함, total count 쿼리 추가 실행
     * fetchCount : count 쿼리로 변경해서 count 수 조회
     */
    @Test
    public void resultFetch() {
        List<Member> fetch = queryFactory
                .selectFrom(member)
                .fetch();

        Member fetchOne = queryFactory
                .selectFrom(member)
                .fetchOne();

        Member fetchLimit = queryFactory
                .selectFrom(member)
                //.limit(1).fetchOne()
                .fetchFirst();

        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .fetchResults();

        long total = result.getTotal();
        List<Member> results = result.getResults();

        long totalCount = queryFactory
                .selectFrom(member)
                .fetchCount();

    }

    /**
     * 회원 정렬 순서
     * 1. 회원 나이 내림차순(desc)
     * 2. 회원 이름 올림차순(asc)
     * 단 2에서 회원 이름이 없으면 마지막에 출력(nulls last) 즉 이름이 null이면 마지막에 출력
     */
    @Test
    public void sort() {
        em.persist(new Member(null, 100));
        em.persist(new Member("member5", 100));
        em.persist(new Member("member6", 100));

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(100))
                .orderBy(member.age.desc(), member.username.asc().nullsLast())
                .fetch();

        Member member5 = result.get(0);
        Member member6 = result.get(1);
        Member memberNull = result.get(2);

        assertThat(member5.getUsername()).isEqualTo("member5");
        assertThat(member6.getUsername()).isEqualTo("member6");
        assertThat(memberNull.getUsername()).isNull();
    }

    @Test
    public void paging1() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetch();

        assertThat(result.size()).isEqualTo(2);
    }

    @Test
    public void paging2() {
        QueryResults<Member> result = queryFactory
                .selectFrom(member)
                .orderBy(member.username.desc())
                .offset(1)
                .limit(2)
                .fetchResults();

        assertThat(result.getTotal()).isEqualTo(4);
        assertThat(result.getLimit()).isEqualTo(2);
        assertThat(result.getOffset()).isEqualTo(1);
        assertThat(result.getResults().size()).isEqualTo(2);

    }

    /**
     * 집합
     */
    @Test
    public void aggregation() {
        List<Tuple> result = queryFactory
                .select(
                        member.count(),
                        member.age.sum(),
                        member.age.avg(),
                        member.age.max(),
                        member.age.min()
                )
                .from(member)
                .fetch();

        Tuple tuple = result.get(0);
        assertThat(tuple.get(member.count())).isEqualTo(4);
        assertThat(tuple.get(member.age.sum())).isEqualTo(100);
        assertThat(tuple.get(member.age.avg())).isEqualTo(25);
        assertThat(tuple.get(member.age.max())).isEqualTo(40);
        assertThat(tuple.get(member.age.min())).isEqualTo(10);
    }

    /**
     * 팀의 이름과 각 팀의 평균 연령을 구해라.
     */
    @Test
    public void group() throws Exception {
        List<Tuple> result = queryFactory
                .select(team.name, member.age.avg())
                .from(member)
                .join(member.team, team)
                .groupBy(team.name)
                .fetch();

        Tuple teamA = result.get(0);
        Tuple teamB = result.get(1);

        assertThat(teamA.get(team.name)).isEqualTo("teamA");
        assertThat(teamA.get(member.age.avg())).isEqualTo(15);

        assertThat(teamB.get(team.name)).isEqualTo("teamB");
        assertThat(teamB.get(member.age.avg())).isEqualTo(35);

    }

    /**
     * join
     * 조인의 기본 문법은 첫 번째 파라미터에 조인 대상을 지정하고, 두번쨰 파라미터에 별칭으로 사용할 Q 타입을 지정하면 된다.
     * 팀 A에 소속된 모든 회원
     */
    @Test
    public void join() {
        List<Member> result = queryFactory
                .selectFrom(member)
                .join(member.team, team)
                .where(team.name.eq("teamA"))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("member1", "member2");

    }

    /**
     * 연관관계가 없어도 join을 하는 방법
     * 조건 : 회원의 이름이 팀 이름과 같은 회원 조회
     * from절에 여러 엔티티를 선택해서 세타 조인
     * 외부 조인 불가능 > 다음에 설명할 조인 on을 사용하면 외부조인 가능
     */
    @Test
    public void theta_join() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));

        List<Member> result = queryFactory
                .select(member)
                .from(member, team)
                .where(member.username.eq(team.name))
                .fetch();

        assertThat(result)
                .extracting("username")
                .containsExactly("teamA", "teamB");
    }

    /**
     * 조인 On 절(JPA 2.1부터 지원)
     * 기능
     * 1) 조인 대상 필터링
     * 조건 : 회원과 팀을 조인하면서, 팀 이름이 teamA인 팀만 조인, 회원은 모두 조회
     */
    @Test
    public void join_on_filtering() {
        List<Tuple> result = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(member.team, team) // 이 경우 자동으로 qeurydsl에서 id로 on절을 만들어 줌. on member0_.team_id=team1_.id
                .on(team.name.eq("teamA")) // and (team1_.name=?)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * 조인 On 절(JPA 2.1부터 지원, 하이버네이트 5.1지원)
     * 기능
     * 2) 연관관계가 없는 엔티티를 외부 조인 할때
     * 조건 : 회원의 이름이 팀 이름과 같은 대상을 외부조인 해라
     */
    @Test
    public void join_on_no_relation() {
        em.persist(new Member("teamA"));
        em.persist(new Member("teamB"));
        em.persist(new Member("teamC"));

        List<Tuple> fetch = queryFactory
                .select(member, team)
                .from(member)
                .leftJoin(team)
                .on(member.username.eq(team.name))
                .fetch();

        for (Tuple tuple : fetch) {
            System.out.println("tuple = " + tuple);
        }

    }

    /**
     * fetch 조인
     */
    @PersistenceUnit
    EntityManagerFactory emf;

    @Test
    public void fetchJoinNo() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .where(member.username.eq("member1"))
                .fetchOne();

        /**
         * find member에 있는 team이 이미 로딩된 Entity 인지 초기화된 Entity인지(false)가 나올것임.
         * 현재 패치조인이 적용되지 않았기 때문에 로딩되지 않는것이 맞음.(team을 touch하면 로딩이 될것임)
         */
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 미적용").isFalse();
    }

    @Test
    public void fetchJoin() {
        em.flush();
        em.clear();

        Member findMember = queryFactory
                .selectFrom(member)
                .join(member.team, team).fetchJoin()
                .where(member.username.eq("member1"))
                .fetchOne();


        /**
         * find member에 있는 team이 이미 로딩된 Entity 인지 초기화된 Entity인지(false)가 나올것임.
         * 현재 패치조인이 적용되어 있고, join으로 인해 team을 가져오기 때문에 load가 된 상태임(여기까지는 한번의 쿼리로 다 가져옴)
         */
        boolean loaded = emf.getPersistenceUnitUtil().isLoaded(findMember.getTeam());
        assertThat(loaded).as("패치 조인 적용").isTrue();
    }

    /**
     * 서브쿼리(com.querydsl.jpa.JPAExpressions 사용)
     * 조건 : 나이가 가장 많은 회원 조회
     * subQuery의 경우 안에 있는 select 문의 객체가 바깥의 객체와 겹치면 안된다 그래서 as를 별도로 만들어 줘야 함.
     */
    @Test
    public void subQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.eq(
                        JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result).extracting("age")
                .containsExactly(40);
    }

    /**
     * 서브쿼리(com.querydsl.jpa.JPAExpressions 사용)
     * 조건 : 나이가 평균 이상인 회원
     * subQuery의 경우 안에 있는 select 문의 객체가 바깥의 객체와 겹치면 안된다 그래서 as를 별도로 만들어 줘야 함.
     */
    @Test
    public void subQueryGoe() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.goe(
                        JPAExpressions
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ))
                .fetch();

        assertThat(result.size()).isEqualTo(2);
        assertThat(result).extracting("age")
                .containsExactly(30, 40);
    }

    /**
     * 서브쿼리(com.querydsl.jpa.JPAExpressions 사용)
     * 조건 : 나이가 평균 이상인 회원(in 사용)
     * subQuery의 경우 안에 있는 select 문의 객체가 바깥의 객체와 겹치면 안된다 그래서 as를 별도로 만들어 줘야 함.
     */
    @Test
    public void subQueryIn() {
        QMember memberSub = new QMember("memberSub");

        List<Member> result = queryFactory
                .selectFrom(member)
                .where(member.age.in(
                        JPAExpressions
                                .select(memberSub.age)
                                .from(memberSub)
                                .where(memberSub.age.gt(10)) //10살 초과
                ))
                .fetch();

        assertThat(result.size()).isEqualTo(3);
        assertThat(result).extracting("age")
                .containsExactly(20, 30, 40);
    }

    /**
     * Select절 서브쿼리(com.querydsl.jpa.JPAExpressions 사용)
     * subQuery의 경우 안에 있는 select 문의 객체가 바깥의 객체와 겹치면 안된다 그래서 as를 별도로 만들어 줘야 함.
     */

    /**
     * 한계점 :JPA JPQL 서브쿼리의 한계점으로 from 절의 서브쿼리(인라인 뷰)는 지원하지 않는다.
     *          Querydsl 마찬기지로, 지원하지 않으며, 하이버네이트 구현체를 사용하여 selct 절의 서브쿼리는 지원이 가능하다.
     * 해결방안 : 서브쿼리를 join으로 변경(가능한 상황도 있지만, 불가능한 상황도 있음.)
     *          애플리케이션에서 쿼리를 2번 분리해서 실행한다.
     *          nativeSQL을 사용한다.
     */
    @Test
    public void selectSubQuery() {
        QMember memberSub = new QMember("memberSub");

        List<Tuple> result = queryFactory
                .select(member.username,
                        JPAExpressions //staticImport 가능
                                .select(memberSub.age.avg())
                                .from(memberSub)
                ).from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    /**
     * case문
     * 간단한 when then 문법
     */
    @Test
    public void basicCase() {
        List<String> result = queryFactory
                .select(member.age
                        .when(10).then("열살")
                        .when(20).then("슴살")
                        .otherwise("기타")
                ).from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * case문
     * 복잡한 조건(CaseBuilder를 사용함.)
     */
    @Test
    public void complexCase() {
        List<String> result = queryFactory
                .select(new CaseBuilder()
                        .when(member.age.between(0, 20)).then("0~20살")
                        .when(member.age.between(21, 23)).then("21~30살")
                        .otherwise("기타")
                ).from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 상수, 문자 더하기
     */
    @Test
    public void constant() {
        List<Tuple> result = queryFactory
                .select(member.username, Expressions.constant("A"))
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            System.out.println("tuple = " + tuple);
        }
    }

    @Test
    public void concat() {
        List<String> result = queryFactory
                .select(member.username.concat("_").concat(member.age.stringValue()))
                .from(member)
                .where(member.username.eq("member1"))
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    /**
     * 프로젝션 : select에 대상을 지정하는 것.
     * 프로젝션 대상이 하나면 타입을 명확하게 지정할 수 있음.
     * 둘 이상일 경우 튜플이나 DTO로 조회
     */

    @Test
    public void 프로젝션_대상이하나 () {  //select 부분에 객체를 넣어도 객체하나를 조회하는 것과 같음.
        List<String> result = queryFactory
                .select(member.username)
                .from(member)
                .fetch();

        for (String s : result) {
            System.out.println("s = " + s);
        }
    }

    @Test
    public void 프로젝션_튜플() {
        List<Tuple> result = queryFactory
                .select(member.username, member.age)
                .from(member)
                .fetch();

        for (Tuple tuple : result) {
            String username = tuple.get(member.username);
            Integer age = tuple.get(member.age);
            System.out.println("username = " + username);
            System.out.println("age = " + age);

            //Tuple은 둘 이상의 타입이 있을경우 querydsl에서 지원하는 타입인데 이는 repository에서만 사용하고 service 단계(비지니스 로직)까지 가져
            // 가는것은 좋지 못한 설계임.
        }
    }

    /**
     * 순수 JPA에서 DTO를 조회할 떄는 new 명령어를 사용해야함.
     * DTO의 package 이름을 다적어야 해서 지저분함.
     * 생성자 방식만 지원함.
     */
    @Test
    public void 프로젝션_dto_JPQL() {
        List<MemberDto> resultList = em.createQuery("select new study.querydsl.dto.MemberDto(m.username, m.age) from Member m", MemberDto.class)
                .getResultList();//jpql 방법

        for (MemberDto memberDto : resultList) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * Querydsl의 경우
     * 1) 프로퍼티 접근
     * 2) 필드 직접 접근
     * 3) 생성자 사용
     */
    @Test
    public void 프로젝션_dto_Querydsl_setter방식() {  //1)프로퍼티 접근방법, @기본생성자 필수!!!
        List<MemberDto> result = queryFactory
                .select(Projections.bean(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }

    }

    @Test
    public void 프로젝션_dto_Querydsl_Field방식() {  //2)필드 접근방법 //getter, setter가 없어도 됨. 그냥 필드에 값을 넣어버림
        List<MemberDto> result = queryFactory
                .select(Projections.fields(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    @Test
    public void 프로젝션_dto_Querydsl_Constructor방식() {  //3)생성자 접근방법(DTO 객체가 바뀌어도 타입이 맞다면 괜찮다)
        List<MemberDto> result = queryFactory
                .select(Projections.constructor(MemberDto.class, member.username, member.age))
                .from(member)
                .fetch();

        for (MemberDto memberDto : result) {
            System.out.println("memberDto = " + memberDto);
        }
    }

    /**
     * UserDto의 경우 usernmae filed가 없고 name이라는 필드가 있음.
     * 이럴경우 member.usernmae으로 조회시 해당 필드 값에 매칭을 하지 못해 null이 나옴.
     * 이를 위해 .as("name")으로 해결이 가능함.
     */
    @Test
    public void findUserDto() {
        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),
                        member.age))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

    @Test
    public void 서브쿼리일경우DTO_반환() {
        QMember memberSub = new QMember("memberSub");

        List<UserDto> result = queryFactory
                .select(Projections.fields(UserDto.class,
                        member.username.as("name"),

                        ExpressionUtils.as(JPAExpressions
                                .select(memberSub.age.max())
                                .from(memberSub), "age")
                ))
                .from(member)
                .fetch();

        for (UserDto userDto : result) {
            System.out.println("userDto = " + userDto);
        }
    }

}
