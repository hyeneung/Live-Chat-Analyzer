import axios from 'axios';

const instance = axios.create({
  baseURL: process.env.VUE_APP_BACKEND_URL,
  withCredentials: true, // Send cookies with requests
});

// Request Interceptor: Attach access token to headers
instance.interceptors.request.use(
  (config) => {
    // If skipAuth is true, do not attach the Authorization header
    if (config.skipAuth) {
      return config;
    }

    const accessToken = localStorage.getItem('accessToken');
    if (accessToken) {
      config.headers.Authorization = `Bearer ${accessToken}`;
    }
    return config;
  },
  (error) => {
    return Promise.reject(error);
  }
);

// Response Interceptor: Handle 401 errors and token re-issue
instance.interceptors.response.use(
  (response) => {
    return response;
  },
  async (error) => {
    const originalRequest = error.config;

    // If error is 401 and not a re-issue request itself
    if (error.response.status === 401 && !originalRequest._retry) {
      originalRequest._retry = true; // Mark request as retried

      try {
        const refreshToken = localStorage.getItem('refreshToken');
        if (!refreshToken) {
          // No refresh token, dispatch event to show login modal
          console.error('No refresh token found. Dispatching relogin-required event.');
          window.dispatchEvent(new Event('relogin-required'));
          return Promise.reject(error);
        }

        // Request new access token using refresh token
        const response = await axios.post(
          `${process.env.VUE_APP_BACKEND_URL}/api/v1/users/reissue`,
          { refreshToken: refreshToken },
          { withCredentials: true }
        );

        const { accessToken: newAccessToken, refreshToken: newRefreshToken } = response.data;

        // Update tokens in localStorage
        localStorage.setItem('accessToken', newAccessToken);
        localStorage.setItem('refreshToken', newRefreshToken);

        // Update the authorization header for the original request
        instance.defaults.headers.common['Authorization'] = `Bearer ${newAccessToken}`;
        originalRequest.headers['Authorization'] = `Bearer ${newAccessToken}`;

        // Retry the original request
        return instance(originalRequest);
      } catch (reissueError) {
        console.error('Token re-issue failed:', reissueError);
        // Re-issue failed, clear tokens and dispatch event to show login modal
        localStorage.removeItem('accessToken');
        localStorage.removeItem('refreshToken');
        localStorage.removeItem('userName');
        localStorage.removeItem('userProfileImage');
        window.dispatchEvent(new Event('relogin-required'));
        return Promise.reject(reissueError);
      }
    }

    return Promise.reject(error);
  }
);

export default instance;