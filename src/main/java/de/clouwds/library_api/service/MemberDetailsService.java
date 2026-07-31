package de.clouwds.library_api.service;

import de.clouwds.library_api.model.Member;
import de.clouwds.library_api.model.MemberPrincipal;
import de.clouwds.library_api.repository.MemberRepository;
import org.jspecify.annotations.NonNull;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class MemberDetailsService implements UserDetailsService {

    private final MemberRepository memberRepository;

    public MemberDetailsService(MemberRepository memberRepository) {
        this.memberRepository = memberRepository;
    }

    @Override
    public UserDetails loadUserByUsername(@NonNull String email) throws UsernameNotFoundException {
        Member member = memberRepository.findByEmail(email).orElseThrow(() -> new UsernameNotFoundException("Could not find user with email " + email));
        return new MemberPrincipal(member);

        /*

        Old implementation with UserBuilder before using custom implementation of UserDetails
        -> needed to access memberId for Method Level Security

        //authorities can actually also be passed as a String, but this is for learning
        String role = "ROLE_" + member.getRole().name();
        GrantedAuthority grantedAuthority = new SimpleGrantedAuthority(role);

        //User.UserBuilder user = User.withUsername(member.getEmail());
        User.UserBuilder userBuilder = User.builder();
        userBuilder.username(member.getEmail());
        userBuilder.password(member.getPassword());
        userBuilder.authorities(grantedAuthority);

        return userBuilder.build();
        */
    }

}
