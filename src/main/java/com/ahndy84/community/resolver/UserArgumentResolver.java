package com.ahndy84.community.resolver;

import com.ahndy84.community.annotation.SocialUser;
import com.ahndy84.community.domain.User;
import com.ahndy84.community.repository.UserRepository;
import org.springframework.core.MethodParameter;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.provider.OAuth2Authentication;
import org.springframework.stereotype.Component;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import static com.ahndy84.community.domain.enums.SocialType.KAKAO;

import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Component
public class UserArgumentResolver implements HandlerMethodArgumentResolver {

	private UserRepository userRepository;
	public UserArgumentResolver(UserRepository userRepository) {
		this.userRepository = userRepository;
	}

	/**
	 * HandlerMethodArgumentResolver가 해당하는 파라미터를 지원할지 여부를 반환한다.
	 * true를 반환하면 resolveArgument 메서드가 수행됩니다.
	 * @param parameter
	 * @return
	 */
	public boolean supportsParameter(MethodParameter parameter){
		return parameter.getParameterAnnotation(SocialUser.class) != null && parameter.getGenericParameterType().equals(User.class);
	}

	/**
	 * 파라미터의 인잣값에 대한 정보를 바탕으로 실제 객체를 생성하여 해당 파라미터 객체에 바인딩합니다.
	 * 검증이 완료된 파라미터 정보를 받습니다.
	 * @param parameter
	 * @param mavContainer
	 * @param webRequest
	 * @param binderFactory
	 * @return
	 * @throws Exception
	 */
	public Object resolveArgument(MethodParameter parameter, ModelAndViewContainer mavContainer, NativeWebRequest webRequest,
	                              WebDataBinderFactory binderFactory) throws  Exception{
		HttpSession session = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest().getSession();
		User user = (User) session.getAttribute("user");
		return getUser(user, session);
	}


	/**
	 * 인증된 User 객체를 만드는 메인 메서드
	 * @param user
	 * @param session
	 * @return
	 */
	private User getUser(User user, HttpSession session) {
		if(user == null) {
			try {
				OAuth2Authentication authentication = (OAuth2Authentication) SecurityContextHolder.getContext().getAuthentication();

				Map<String, String> map = (HashMap<String, String>) authentication.getDetails();

				/*
				SocialType, 즉 어떤 소셜 미디어로 인증받았는지 String.valueOf(authentication.getAuthorities().toArray()[0])으로 불러옵니다.
				이전에 넣어주었던 권한이 하나뿐이라서 배열의 첫번째 값만 불러오도록 작성하였습니다.
				 */
				User convertUser = convertUser( String.valueOf(authentication.getAuthorities().toArray()[0]), map );

				user = userRepository.findByEmail(convertUser.getEmail());
				if(user == null) {
					user = userRepository.save(convertUser);
				}

				setRoleINotSame(user, authentication, map);
				session.setAttribute("user", user);
			} catch (ClassCastException e) {
				return user;
			}
		}
		return user;
	}

	/**
	 * 사용자의 인증된 소셜 미디어 타입에 따라 빌더를 사용하여 User 객체를 만들어 주는 가교역할을 합니다.
	 * 카카오의 경우 에는 별도의 메서드를 사용합니다.
	 * @param authority
	 * @param map
	 * @return
	 */
	private User convertUser(String authority, Map<String, String> map) {
		if(KAKAO.isEquals(authority)) return getKakaoUser(map);
		return null;
	}

	private User getKakaoUser(Map<String, String> map) {
		HashMap<String, String> propertyMap = (HashMap<String, String>)(Object)map.get("properties");
		return User.builder()
				.name(propertyMap.get("nickname"))
				.email(map.get("kaccount_email"))
				.principal(String.valueOf(map.get("id")))
				.socialType(KAKAO)
				.createdDate(LocalDateTime.now())
				.build();
	}

	/**
	 * 인증된 authentication이 권한을 갖고 있는지 체크하는 용도로 쓰입니다.
	 * 만약 저장된 User 권한이 없으면 SecurityContextHolder를 사용하여 해달 소셜 미디어 타입으로 권한을 저장합니다.
	 * @param user
	 * @param authentication
	 * @param map
	 */
	private void setRoleINotSame(User user, OAuth2Authentication authentication, Map<String, String> map) {
		if(!authentication.getAuthorities().contains(new SimpleGrantedAuthority(user.getSocialType().getRoleType()))) {
			SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(map, "N/A",
					AuthorityUtils.createAuthorityList(user.getSocialType().getRoleType())));
		}
	}
}
