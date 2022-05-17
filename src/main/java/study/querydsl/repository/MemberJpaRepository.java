package study.querydsl.repository;

import com.querydsl.jpa.impl.JPAQueryFactory;
import org.springframework.stereotype.Repository;
import study.querydsl.entity.Member;
import study.querydsl.entity.QMember;

import javax.persistence.EntityManager;
import java.util.List;
import java.util.Optional;

import static study.querydsl.entity.QMember.*;

/**
 * 순수 JPA 리포지토리와 Querydsl
 */
@Repository
public class MemberJpaRepository {

    private final EntityManager em;
    private final JPAQueryFactory queryFactory;

    //Spring Bean등록을 해도 무방 //장점 : test Code 작성시 주입받을 필요가 없다  // 단점 : 귀찮음
    public MemberJpaRepository(EntityManager em) {
        this.em = em;
        this.queryFactory = new JPAQueryFactory(em);
    }
    
//      Srping Bean등록시 생성자 // 장점 : Lombok의 @requiredArgsConstructor 등을 사용하기 편리하다 // 단점 : Test code 작성시 @Autowired를 통해 주입받아야 한다
//    public MemberJpaRepository(EntityManager em, JPAQueryFactory queryFactory) {
//        this.em = em;
//        this.queryFactory = queryFactory;
//    }


    //Srpig Bean 등록시(QuerydslApplication.java 참조

    public void save(Member member) {
        em.persist(member);
    }

    public Optional<Member> findById(Long id) {
        Member findMember = em.find(Member.class, id);
        return Optional.ofNullable(findMember);
    }

    public List<Member> findAll() {
        return em.createQuery("select m from Member m", Member.class)
                .getResultList();
    }

    public List<Member> findAll_Querydsl() {
        return queryFactory
                .selectFrom(member)
                .fetch();
    }

    public List<Member> findByUsername(String username) {
        return em.createQuery("select  m from Member m where m.username = :username", Member.class)
                .setParameter("username", username)
                .getResultList();
    }

    public List<Member> findByUsername_Querydsl(String username) {
        return queryFactory
                .selectFrom(member)
                .where(member.username.eq(username))
                .fetch();
    }
}
